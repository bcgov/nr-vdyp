package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ca.bc.gov.nrs.vdyp.io.parse.common.InvalidGenusDistributionSet;

/**
 * A Sp64DistributionSet contains <code>maxIndex</code> (>= 0) {@link Sp64Distribution} instances. The provided list of
 * Sp64Distributions is sorted in increasing index order. The resulting list must have percentages in decreasing order.
 * <p>
 * The percentage values PCT1 + PCT2 + PCT3 + PCT4 should, but not must, sum to 100. The PCT’s should be in decreasing
 * order: PCT1 >= PCT2 >= PCT3 >= PCT4. This is because the first will be used when assigning the site curve number
 * (SCN) if not provided directly by the user and so must be the dominant sp64.
 */
public class Sp64DistributionSet implements Comparable<Sp64DistributionSet> {

	private Map<Integer, Sp64Distribution> sp64DistributionMap = new HashMap<>();

	/** The Sp64Distributions, sorted by increasing index */
	private List<Sp64Distribution> sp64DistributionList = new ArrayList<>();

	/** The highest index in the given distributions */
	private final int maxIndex;

	/**
	 * Builds an {@link Sp64DistributionSet} whose maxIndex is the maximum index that appears in the given list of
	 * {@link Sp64Distribution}.
	 *
	 * @param sdList the {@link Sp64Distribution} from which the set is to be constructed.
	 */
	public Sp64DistributionSet(List<Sp64Distribution> sdList) {

		this(
				(sdList == null || sdList.isEmpty()) ? 0
						: sdList.stream().max((o1, o2) -> o1.getIndex() - o2.getIndex()).get().getIndex(),
				sdList
		);
	}

	public Sp64DistributionSet(int maxIndex, List<Sp64Distribution> sdList) {

		// sort the list by increasing index
		var sp64DistributionListSortedByIndex = sdList.stream().sorted((o1, o2) -> o1.getIndex() - o2.getIndex())
				.toList();

		try {
			validate(maxIndex, sp64DistributionListSortedByIndex);
		} catch (InvalidGenusDistributionSet e) {
			throw new IllegalArgumentException(e);
		}

		this.sp64DistributionList = sp64DistributionListSortedByIndex;
		this.maxIndex = maxIndex;
		for (Sp64Distribution sd : sdList) {
			sp64DistributionMap.put(sd.getIndex(), sd);
		}
	}

	private Sp64DistributionSet(Sp64DistributionSet other) {

		this.sp64DistributionList = new ArrayList<>(other.sp64DistributionList);
		this.maxIndex = other.maxIndex;

		for (Sp64Distribution sd : this.sp64DistributionList) {
			sp64DistributionMap.put(sd.getIndex(), sd);
		}
	}

	/**
	 * Construct an empty Sp64DistributionSet. Usefulness is limited to testing.
	 */
	public Sp64DistributionSet() {
		maxIndex = 0;
	}

	public List<Sp64Distribution> getSp64DistributionList() {

		return Collections.unmodifiableList(sp64DistributionList);
	}

	public Map<Integer, Sp64Distribution> getSp64DistributionMap() {

		return Collections.unmodifiableMap(sp64DistributionMap);
	}

	public int getSize() {
		return sp64DistributionList.size();
	}

	public Optional<Sp64Distribution> getSpeciesDistribution(int withIndex) {
		if (withIndex > maxIndex) {
			throw new IllegalArgumentException(
					MessageFormat.format("Index argument {0} exceeds the maximum value {1}", withIndex, maxIndex)
			);
		}
		if (sp64DistributionMap.containsKey(withIndex))
			return Optional.of(sp64DistributionMap.get(withIndex));
		else
			return Optional.empty();
	}

	public Sp64DistributionSet copy() {
		return new Sp64DistributionSet(this);
	}

	private static void validate(int maxIndex, List<Sp64Distribution> gdList) throws InvalidGenusDistributionSet {

		Set<String> sp64sSeen = new HashSet<>();
		Set<Integer> indicesSeen = new HashSet<>();

		Sp64Distribution prevGd = null;

		for (Sp64Distribution gd : gdList) {
			if (sp64sSeen.contains(gd.getGenusAlias())) {
				throw new InvalidGenusDistributionSet(
						MessageFormat.format(
								"Species {0} appears more than once in GenusDistributionSet", gd.getGenusAlias()
						)
				);
			}
			if (indicesSeen.contains(gd.getIndex())) {
				throw new InvalidGenusDistributionSet(
						MessageFormat.format("Index {0} appears more than once in GenusDistributionSet", gd.getIndex())
				);
			}
			if (gd.getIndex() < 1 || gd.getIndex() > maxIndex) {
				throw new InvalidGenusDistributionSet(
						MessageFormat.format(
								"Index {0} is out of range - acceptable values are between 1 and {1}, inclusive", gd
										.getIndex(), maxIndex
						)
				);
			}

			if (prevGd != null && prevGd.getPercentage() < gd.getPercentage()) {
				throw new InvalidGenusDistributionSet(
						MessageFormat.format(
								"The percentage of index {0} is {1} and is greater than {2}, that of a species distribution with a lower index",
								gd
										.getIndex(), maxIndex
						)
				);
			}

			sp64sSeen.add(gd.getGenusAlias());
			indicesSeen.add(gd.getIndex());

			prevGd = gd;
		}
	}

	@Override
	public int hashCode() {
		return sp64DistributionMap.hashCode() * 17 + maxIndex;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Sp64DistributionSet that) {
			return compareTo(that) == 0;
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(Sp64DistributionSet that) {

		if (that != null) {
			if (this.maxIndex != that.maxIndex) {
				return this.maxIndex - that.maxIndex;
			}

			var thisIterator = this.sp64DistributionList.iterator();
			var thatIterator = that.sp64DistributionList.iterator();

			while (thisIterator.hasNext()) {
				if (!thatIterator.hasNext()) {
					return 1 /* this is longer than that */;
				}

				var thisElement = thisIterator.next();
				var thatElement = thatIterator.next();
				int result = thisElement.compareTo(thatElement);
				if (result != 0) {
					return result;
				}
			}

			if (thatIterator.hasNext()) {
				return -1 /* this is shorter than that */;
			}

			return 0;
		} else {
			// null is less than non-null, per ObjectUtils.compare()
			return 1;
		}
	}
}
