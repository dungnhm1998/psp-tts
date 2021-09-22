package asia.leadsgen.psp.service;

import java.io.File;
import java.util.logging.Logger;

import org.springframework.aop.ThrowsAdvice;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

public class AmazonS3Service {
	
	private static String awsAccessKeyId;
	private static String awsSecretKeyId;
	private static String awsRegion;
	private static String awsCdn;
	private static String awsBucket;
	
	private static AmazonS3 s3client = null;
	
	public static String uploadFile (String keyName, File file) {
		
		String fileURL = "";
		
		TransferManager xferManager = TransferManagerBuilder.standard()
				.withS3Client(getClient()).build();
		
		try {
			
			Upload xfer = xferManager.upload(awsBucket,keyName,file);
			UploadResult result = xfer.waitForUploadResult();
			fileURL = awsCdn + result.getKey();
			logger.info("Upload file to aws3 success with url " + fileURL);
			
		} catch (AmazonServiceException e) {
			logger.severe("Amazon service error: " + e.getMessage());
        } catch (AmazonClientException e) {
        	logger.severe("Amazon client error: " + e.getMessage());
        } catch (InterruptedException e) {
        	logger.severe("Transfer interrupted: " + e.getMessage());
        }
		
		xferManager.shutdownNow(false);
		
		return fileURL;
	}
	
	public static AmazonS3 getClient() {
		
		if (s3client == null) {
			
			AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKeyId);
			AmazonS3 client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(awsRegion)
                    .build();
			s3client = client;
		}
		
		return s3client;
	}
	
	public static void setAwsAccessKeyId(String awsAccessKeyId) {
		AmazonS3Service.awsAccessKeyId = awsAccessKeyId;
	}
	public static void setAwsSecretKeyId(String awsSecretKeyId) {
		AmazonS3Service.awsSecretKeyId = awsSecretKeyId;
	}
	public static void setAwsRegion(String awsRegion) {
		AmazonS3Service.awsRegion = awsRegion;
	}
	public static void setAwsCdn(String awsCdn) {
		AmazonS3Service.awsCdn = awsCdn;
	}

	public static void setAwsBucket(String awsBucket) {
		AmazonS3Service.awsBucket = awsBucket;
	}

	private static final Logger logger = Logger.getLogger(AmazonS3Service.class.getName());
}
