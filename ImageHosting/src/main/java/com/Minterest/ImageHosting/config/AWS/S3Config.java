package com.Minterest.ImageHosting.config.AWS;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class S3Config {
    @Value("${cloud.aws.credentials.access-key}")
    private String awsAccessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String awsSecretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${app.s3.multipart.threshold:5}")
    private long multipartUploadThreshold;

    @Value("${app.s3.thread.pool.size:10}")
    private int threadPoolSize;

   @Bean
   @Primary
    public AmazonS3 amazonS3Client(){
       AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
       return AmazonS3ClientBuilder.standard()
               .withCredentials(new AWSStaticCredentialsProvider(credentials))
               .withRegion(region)
               .enablePathStyleAccess()
               .build();
   }
   @Bean
    public AmazonS3ClientBuilder amazonS3ClientBuilder(){
       return AmazonS3ClientBuilder.standard();
   }
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService transferManagerExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    @Bean(destroyMethod = "shutdownNow")
    public TransferManager transferManager(AmazonS3 amazonS3, ExecutorService transferManagerExecutor) {
        return TransferManagerBuilder.standard()
                .withS3Client(amazonS3)
                .withMultipartUploadThreshold(multipartUploadThreshold * 1024 * 1024L) // MB threshold
                .withExecutorFactory(() -> transferManagerExecutor)
                .build();
    }
}
