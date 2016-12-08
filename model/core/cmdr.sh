#!/bin/bash

ORIG=`pwd`

OUTPUT=$ORIG/mmds.md5

# CreateMemoryDiskReferences

# Create mmd*.dat, for use with MutatedMemoryDisksTest unit test.

# Files ones_20 and rnd21_30 are first created by running
# the CreateMemoryDiskReferencesTest unit test.

# 1 GB of zeros, with mutated region of 1MB of 1s written at start
dd if=/dev/zero of=mmd1.dat bs=1M count=1K
dd if=ones_20   of=mmd1.dat conv=notrunc

# 1 GB of randoms (seed 21), with mutated region of 1MB of 1s written near start
cp rnd21_30 mmd2.dat
dd if=ones_20 of=mmd2.dat bs=1M seek=8 conv=notrunc
#dd if=ones_20 of=mmd2.dat conv=notrunc

# 1 GB of randoms (seed 21), with TWO mutated regions:
# A 1MB of 1s at 8MB, then 16MB of 0s at 32MB
cp rnd21_30 mmd3.dat
dd if=ones_20 of=mmd3.dat bs=1M seek=8 conv=notrunc
dd if=/dev/zero of=mmd3.dat bs=1M count=16 seek=32 conv=notrunc
#dd if=ones_20 of=mmd2.dat conv=notrunc

md5sum *.dat > $OUTPUT

# Short of space on main drive, using 4TB external for BIG disks
EXTERNAL = /media/stuart/719ebe5f-c5aa-4d53-a481-eafc8b323c5f/ 
[ -d $EXTERNAL] || exit
cd $EXTERNAL

# 4 GB of randoms (seed 21), with TWO mutated regions:
# A 1MB of 1s at 8MB, then 16MB of 0s at 32MB
cp rnd21_32 mmd4.dat
md5sum *.dat >> $OUTPUT

# Advertise what we just computed...
cat $OUTPUT


# eof
