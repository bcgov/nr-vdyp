# GNU awk: normalize each CLF timestamp to UTC, including its numeric offset.
BEGIN { split("Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec", names); for (i in names) months[names[i]] = i }
{
    total++
    if (!match($0, /\[([0-9]{2})\/([A-Za-z]{3})\/([0-9]{4}):([0-9]{2}):([0-9]{2}):([0-9]{2}) ([+-])([0-9]{2})([0-9]{2})\]/, t) || !(t[2] in months)) {
        print "Unrecognized access timestamp" > "/dev/stderr"; failed = 1; exit 1
    }
    epoch = mktime(t[3] " " months[t[2]] " " t[1] " " t[4] " " t[5] " " t[6], 1)
    offset = (t[8] * 60 + t[9]) * 60
    epoch -= (t[7] == "+" ? offset : -offset)
    if (epoch >= start && epoch < end) { print; selected++ }
}
END { if (!failed) print total+0, selected+0 > counts }
