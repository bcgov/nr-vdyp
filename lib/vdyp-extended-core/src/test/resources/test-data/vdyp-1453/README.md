# VDYP-1453

`polygons.csv` and `layers.csv` contain the supplied polygon 23116780 and its two
layers without changing their data. The preceding valid polygon 13919428 comes
from `test-data/hcsv/common`; 23116781 is a copy of that valid polygon with only
its feature ID changed, to exercise continuation after a failure. These are not
the unavailable data for polygon 10307929.

`ITProjectionRunner` processes these three polygons together using the real
projection components and CSV yield-table writer. The unmodified dataset is also
tested through the public `run` method. Since the supplied polygon does not fail
for the test's 2013–2014 range, the other cases use the component-runner overload
to inject either a `YieldTableGenerationException` or an unexpected runtime
exception at yield generation for 23116780. They verify that the error is logged
once with the feature ID and that both neighbouring polygons retain all their
yield rows.

Run from the repository root:

```shell
mvn -pl lib/vdyp-extended-core -am verify -DskipUnitTests=true -Dit.test=ITProjectionRunner -Dfailsafe.failIfNoSpecifiedTests=false
```
