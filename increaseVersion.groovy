
version=args[0]

split=version.split("\\.")
newMinor=split[2].toInteger() + 1
split[2]=newMinor

println(split.join("."))

