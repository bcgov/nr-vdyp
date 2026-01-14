package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ca.bc.gov.nrs.vdyp.batch.client.coms.ComsClient;
import ca.bc.gov.nrs.vdyp.batch.client.coms.PresignedFileFetcher;

@Service
public class ComsFileService {
	private final ComsClient comsClient;
	private final PresignedFileFetcher presignedFileFetcher;

	public ComsFileService(ComsClient comsClient, PresignedFileFetcher presignedFileFetcher) {
		this.comsClient = comsClient;
		this.presignedFileFetcher = presignedFileFetcher;
	}

	public void fetchObjectToFile(UUID objectId, Path target) throws IOException {
		String url = comsClient.getPresignedUrl(objectId.toString());
		presignedFileFetcher.downloadToFile(url, target);
	}
}
