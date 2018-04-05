#include <pushUtil.h>
#include <sys/time.h>

int64_t getCurrentTime()      //直接调用这个函数就行了，返回值最好是int64_t，long long应该也可以
{
    struct timeval tv;
    gettimeofday(&tv, NULL);    //该函数在sys/time.h头文件中
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

uint8_t videoFrameIsKey(char *data) {
    int a = data[1] & 0x1F;
    if (a == 5 || a == 7 || a == 8 || a == 2)
        return 1;
    return 0;
}