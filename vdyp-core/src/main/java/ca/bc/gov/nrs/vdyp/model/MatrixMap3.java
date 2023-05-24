package ca.bc.gov.nrs.vdyp.model;

import java.util.Optional;

public interface MatrixMap3<K1, K2, K3, V> extends MatrixMap<V> {

	default public void put(K1 key1, K2 key2, K3 key3, V value) {
		putM(value, key1, key2, key3);
	}

	default public Optional<V> get(K1 key1, K2 key2, K3 key3) {
		return getM(key1, key2, key3);
	}
}
