package edu.neu.coe.csye6225.cloudnativeapp.service;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import edu.neu.coe.csye6225.cloudnativeapp.domain.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Service
@Profile("dev")
public class AmazonClient implements UploadClient {

    private AmazonS3 s3Client;

    @Value("${amazonProperties.bucketName:s3.csye6225-spring2018-guptaanu.me}")
    private String bucketName;

    @Autowired
    private SecurityServiceImpl securityService;

    private static final String PROFILE_DIR = "Profile_Pics/";

    private static final String FILE_NAME_PRE = "profile_pic_";

    private static final String DOT = ".";


    @PostConstruct
    private void initializeAmazon() {

        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();


    }

    @Override
    public void storeProfilePic(MultipartFile file) {


        UserAccount loggedInUsername = securityService.findLoggedInUsername();
        String[] split = file.getOriginalFilename().split("\\.");
        String contentType = Arrays.asList(split).get(1);
        String id = loggedInUsername.getId().toString();
        String fileName = FILE_NAME_PRE + id + DOT + contentType;


        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        s3Client.putObject(bucketName, PROFILE_DIR + fileName, inputStream, new ObjectMetadata());

    }


    public InputStream getProfilePic() {

        UserAccount loggedInUsername = securityService.findLoggedInUsername();
        String id = loggedInUsername.getId().toString();
        String key = PROFILE_DIR + FILE_NAME_PRE + id;
        // String key = PROFILE_DIR + FILE_NAME_PRE + 2;
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);

        ListObjectsV2Result result = s3Client.listObjectsV2(req);

        String keyName = result.getObjectSummaries().stream()
                .filter(o -> o.getKey().contains(key))
                .map(o -> o.getKey())
                .findAny().orElse(null);

        if (keyName == null) {

            return null;
        }

        S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, keyName));
        return object.getObjectContent();


    }

    public void deleteProfilePic() {


        UserAccount loggedInUsername = securityService.findLoggedInUsername();
        String id = loggedInUsername.getId().toString();
        String key = PROFILE_DIR + FILE_NAME_PRE + id;
        // String key = PROFILE_DIR + FILE_NAME_PRE + 2;
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);

        ListObjectsV2Result result = s3Client.listObjectsV2(req);

        String keyName = result.getObjectSummaries().stream()
                .filter(o -> o.getKey().contains(key))
                .map(o -> o.getKey())
                .findAny().orElse(null);

        if (keyName == null) {

            return;
        }

        s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));


    }


}
