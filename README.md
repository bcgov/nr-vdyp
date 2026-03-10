[![Lifecycle:Experimental](https://img.shields.io/badge/Lifecycle-Experimental-339999)](<Redirect-URL>)
# VDYP
The Variable Density Yield Projection (VDYP) program provides yield predictions for unmanaged B.C. stands in inventory and timber supply applications


## Code formatting

This project uses tabs for indenting so developers can set the indent size however they prefer.

To run the automatic code formatter.

```
mvn formatter:format
```

Ideally, do this before making each commit, but at least do so before marking a PR ready for review.

Remember you can use `git commit --amend` to add formatter changes to the last commit if you forgot or `git commit --fixup=COMMIT_ID` and then `git rebase -i --autosquash main` to make changes to a particular previous commit as long as it hasn't been merged to `main` yet.


To check if the formatter would make changes without actually making them, use

```
mvn formatter:validate
```

The formatting rules used by the formatter are a standard Eclipse formatting profile found at `buildtools/src/main/resources/eclipse/formatter.xml`.  You can load this into Eclipse for make its formatter match the one in Maven.  Still run the maven command to be sure.

Discuss with other developers before making any changes to the formatting profile.

The formatter used to be run as part of the build automatically and some commit s were made before running it.  This can lead to formatting changes if you build from a past commit such as when running `git bisect`.  To disable this behaviour, run the build with `-Dformatter.skip`
