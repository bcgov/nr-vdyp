package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;

public interface MatrixMap2<K1, K2, V> extends MatrixMap<V> {

	default public void put(K1 key1, K2 key2, V value) {
		putM(value, key1, key2);
	}
	
	default public Optional<V> get(K1 key1, K2 key2) {
		return getM(key1, key2);
	}
}
