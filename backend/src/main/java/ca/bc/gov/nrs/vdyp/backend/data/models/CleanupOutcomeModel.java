package ca.bc.gov.nrs.vdyp.backend.data.models;

/** Mirrors the batch service's StorageCleanupService.Outcome enum for deserializing its cleanup report. */
public enum CleanupOutcomeModel {
	DELETABLE, DELETED, PROTECTED, FAILED
}
