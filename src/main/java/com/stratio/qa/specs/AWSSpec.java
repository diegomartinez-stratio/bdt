package com.stratio.qa.specs;

import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.When;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.*;
import java.util.stream.Collectors;

public class AWSSpec extends BaseGSpec {

    public AWSSpec(CommonG spec) {
        this.commonspec = spec;
    }

    /*
     * List S3 buckets and save it in environment variable
     * Need variables aws.accessKeyId and aws.secretAccessKey for connect to AWS S3
     * @param envVar : variable name for save list S3 buckets
     * @throws Exception
     */
    @When("^I list S3 buckets and save it in environment variable '(.+?)'$")
    public void listS3Buckets(String envVar) throws Exception {
        List<String> listBucket = new ArrayList<String>();
        Region region = Region.EU_WEST_1;

        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(SystemPropertyCredentialsProvider.create())
                .build();

        for (Bucket bucket : s3Client.listBuckets().buckets()) {
            listBucket.add(bucket.name());
        }

        ThreadProperty.set(envVar, String.join("\n", listBucket));

        s3Client.close();
    }

    /*
     * List all S3 objects of a bucket recursively for folder or S3 objects of a bucket in the path not recursively, and save it in file
     * Need variables aws.accessKeyId and aws.secretAccessKey for connect to AWS S3
     * @param bucketName: bucket Name
     * @param path: list only the objects in the path not recursively for folder, if base path indicate '/'
     * @param fileName: file name for save list S3 objects
     * @throws Exception
     */
    @When("^I list S3 objects of bucket with name '(.+?)'( in the path '(.+?)')? and save it in file '(.+?)'$")
    public void listBucketObjects(String bucketName, String path, String fileName) throws Exception {
        List<String> listObjectBucket = new ArrayList<String>();
        boolean all = true;
        Region region = Region.EU_WEST_1;

        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(SystemPropertyCredentialsProvider.create())
                .build();

        if (path != null) {
            all = false;
        }

        if (path == null || path.equals("/")) {
            path = "";
        }

        ListObjectsV2Request listObjects = ListObjectsV2Request
                .builder()
                .bucket(bucketName)
                .prefix(path)
                .maxKeys(200)
                .build();

        ListObjectsV2Iterable listRes = s3Client.listObjectsV2Paginator(listObjects);
        SdkIterable<S3Object> objects = listRes.contents();
        for (S3Object myValue : objects) {
            if (!all) {
                String cadena = myValue.key();
                int position = cadena.indexOf(path, 0) + path.length();
                String result = cadena.substring(position, cadena.length());
                if (result.indexOf("/") != -1) {
                    int pos = result.indexOf("/") + 1;
                    result = result.substring(0, pos);
                }
                listObjectBucket.add(result);
            } else {
                listObjectBucket.add(myValue.key());
            }
        }

        listObjectBucket = listObjectBucket.stream().distinct().collect(Collectors.toList());

        String response = String.join("\n", listObjectBucket);

        writeInFile(response, fileName);

        s3Client.close();
    }

    /*
     * Delete S3 objects of a bucket and clear bucket or since specific path
     * Need variables aws.accessKeyId and aws.secretAccessKey for connect to AWS S3
     * @param bucketName : bucket name which delete object in the bucket
     * @throws Exception
     */
    @When("^I clear S3 bucket with name '(.+?)'( in the path '(.+?)')?$")
    public void deleteObjectsBucket(String bucketName, String path) throws Exception {
        Region region = Region.EU_WEST_1;

        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(SystemPropertyCredentialsProvider.create())
                .build();

        if (path == null) {
            path = "";
        }

        ListObjectsV2Request listObjects = ListObjectsV2Request
                .builder()
                .bucket(bucketName)
                .prefix(path)
                .maxKeys(200)
                .build();

        ListObjectsV2Response listObjectsV2Response;

        do {
            listObjectsV2Response = s3Client.listObjectsV2(listObjects);
            for (S3Object s3Object : listObjectsV2Response.contents()) {
                DeleteObjectRequest request = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();
                s3Client.deleteObject(request);
            }
        } while (listObjectsV2Response.isTruncated());

        s3Client.close();
    }

    /*
     * Delete S3 bucket
     * Need variables aws.accessKeyId and aws.secretAccessKey for connect to AWS S3
     * @param bucketName : bucket name to delete
     * @throws Exception
     */
    @When("^I delete S3 bucket with name '(.+?)'$")
    public void deleteBucket(String bucketName) throws Exception {
        Region region = Region.EU_WEST_1;

        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(SystemPropertyCredentialsProvider.create())
                .build();

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
        s3Client.deleteBucket(deleteBucketRequest);

        s3Client.close();
    }

}
