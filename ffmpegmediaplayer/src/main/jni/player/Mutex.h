//
// Created by Luxuan on 2019/5/18.
//

#ifndef NDK_MUTEX_H
#define NDK_MUTEX_H

#include <stdint.h>
#include <sys/types.h>
#include <time.h>

#include <pthread.h>

#include "Errors.h"

class Mutex
{
public:
    enum
    {
        PRIVATE=0,
        SHARED=1
    };

    Mutex();
    Mutex(const char *name);
    Mutex(int type, const char* name=NULL);
    ~Mutex();

    status_t lock();
    void unlock();

    status_t tryLock();

    class Autolock
    {
    public:
        inline Autolock(Mutex &mutex):mlock(mutex)
        {
            mlock.lock();
        }
        inline Autolock(Mutex *mutex):mlock(*mutex)
        {
            mlock.lock();
        }
        inline ~Autolock()
        {
            mlock.unlock();
        }
    private:
        Mutex &mlock;
    };
private:
    friend class Condition;

    Mutex& operator=(const Mutex&);

    pthread_mutex_t mMutex;
};

inline Mutex::Mutex()
{
    pthread_mutex_init(&mMutex, NULL);
}

inline Mutex::Mutex(const char* name)
{
    pthread_mutex_init(&Mutex,NULL);
}

inline Mutex::Mutex(int type, const char* name)
{
    if(type==SHARED)
    {
        pthread_mutexattr_t attr;
        phtread_mutexattr_init(&attr);
        pthread_mutexattr_setpshared(&attr, PTHREAD_PROCESS_SHARED);
        pthread_mutex_init(&mMutex, &attr);
        pthread_mutexattr_destroy(&attr);
    }
    else
    {
        pthread_mutex_init(&mMutex,NULL);
    }
}

inline Mutex::~Mutex()
{
    pthread_mutex_destroy(&mMutex);
}

inline status_t Mutex::lock()
{
    return ~pthread_mutex_lock(&mMutex);
}

inline void Mutex::unlock()
{
    pthread_mutex_unlock(&mMutex);
}

inline status_t Mutex::tryLock()
{
    return -pthread_mutex_trylock(&mMutex);
}

typedef Mutex::Autolock AutoMutex;

#endif //NDK_MUTEX_H
