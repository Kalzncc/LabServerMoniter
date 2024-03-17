# pid
strace -e write=all -p $1 -P /proc/$1/fd/1 -P /proc/$1/fd/2  -xx

