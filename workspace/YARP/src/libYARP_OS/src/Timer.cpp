/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#include <yarp/os/Timer.h>
#include <yarp/os/PeriodicThread.h>
#include <yarp/os/Time.h>
#include <cmath>
#include <map>
#include <mutex>

using namespace yarp::os;

//disclaimer: the following inheritance little madness is for avoiding critical copy and paste code and
//avoiding data inconsistence(example: RateThread::GetIterations() and runTimes)

class yarp::os::Timer::PrivateImpl
{
protected:
    yarp::os::YarpTimerEvent getEventNow(unsigned int iteration);

    bool runTimer(unsigned int iteration, YarpTimerEvent event);

public:
    typedef yarp::os::Timer::TimerCallback TimerCallback;

    PrivateImpl(TimerSettings sett, TimerCallback call, yarp::os::Mutex* mutex = nullptr) :
            m_settings(sett),
            m_callback(call),
            m_mutex(mutex)
    {
    }

    virtual ~PrivateImpl()
    {
    }

    virtual bool startTimer() = 0;

    virtual void stopTimer() = 0;

    virtual bool stepTimer() = 0;

    virtual bool timerIsRunning() = 0;


    TimerSettings    m_settings;
    TimerCallback    m_callback{nullptr};
    double           m_startStamp{0.0},
                     m_lastReal{0.0};
    yarp::os::Mutex* m_mutex;
};

class MonoThreadTimer : public yarp::os::Timer::PrivateImpl
{
public:
    MonoThreadTimer(TimerSettings sett, TimerCallback call, yarp::os::Mutex* mutex = nullptr);
    ~MonoThreadTimer();
    bool         m_active{ false };
    unsigned int m_runTimes{1};
    size_t       m_id{(size_t)-1};

    virtual yarp::os::YarpTimerEvent getEventNow()
    {
        return PrivateImpl::getEventNow(m_runTimes);
    }

    virtual bool startTimer() override
    {
        m_startStamp = yarp::os::Time::now();
        m_active     = true;
        return true;
    }

    virtual void stopTimer() override
    {
        m_active = false;
    }

    virtual bool stepTimer() override
    {
        return step(getEventNow(), true);
    }

    virtual bool step(YarpTimerEvent event, bool singleStep)
    {
        bool m_active = runTimer(m_runTimes, event);
        if (!singleStep) {
            m_runTimes++;
        }
        return m_active;
    }

    virtual bool timerIsRunning() override
    {
        return m_active;
    }
};

class TimerSingleton : public yarp::os::PeriodicThread
{
    std::mutex mu;
    std::map<size_t, MonoThreadTimer*> timers;
    TimerSingleton() :
            PeriodicThread(0.0)
    {
    }

    virtual void run() override;

    virtual ~TimerSingleton()
    {
        stop();
    }

public:
    //reminder: int c++11 static variables'inside function are guaranteed to be lazy initialized and atomic
    static TimerSingleton& self()
    {
        static TimerSingleton instance;
        return instance;
    }

    size_t addTimer(MonoThreadTimer* t)
    {
        mu.lock();
        timers[timers.size()] = t;
        mu.unlock();
        return timers.size() - 1;
    }

    void removeTimer(size_t id)
    {
        mu.lock();
        timers.erase(id);
        mu.unlock();
    }

    size_t getTimerCount()
    {
        return timers.size();
    }
};

MonoThreadTimer::MonoThreadTimer(TimerSettings sett, TimerCallback call, yarp::os::Mutex* mutex) :
        PrivateImpl(sett, call, mutex)
{
    TimerSingleton& singlInstance = TimerSingleton::self();
    m_id = singlInstance.addTimer(this);
    if (!singlInstance.isRunning()) {
        singlInstance.start();
    }
}

MonoThreadTimer::~MonoThreadTimer()
{
    TimerSingleton& singlInstance = TimerSingleton::self();
    singlInstance.removeTimer(m_id);
    if (!singlInstance.getTimerCount()) {
        singlInstance.stop();
    }
}

void TimerSingleton::run()
{
    mu.lock();
    for (auto t : timers) {
        MonoThreadTimer& timer = *t.second;
        YarpTimerEvent tEvent = timer.getEventNow();
        if (timer.m_active && tEvent.currentReal > tEvent.currentExpected) {
            timer.m_active = timer.step(tEvent, false);
            timer.m_lastReal = tEvent.currentReal;
        }
    }
    mu.unlock();
}

class ThreadedTimer : public yarp::os::Timer::PrivateImpl,
                      public yarp::os::PeriodicThread
{
    typedef yarp::os::Timer::TimerCallback TimerCallback;
    virtual void run() override;
    virtual bool threadInit() override;
    bool singleStep{ false };

public:
    ThreadedTimer(TimerSettings sett, TimerCallback call, yarp::os::Mutex* mutex = nullptr) :
            PrivateImpl(sett, call, mutex), PeriodicThread(sett.period)
    {
    }

    virtual ~ThreadedTimer()
    {
        stop();
    }

    virtual bool startTimer() override
    {
        m_startStamp = yarp::os::Time::now();
        return start();
    }

    virtual bool stepTimer() override
    {
        singleStep = true;
        step();
        return true;
    }

    virtual void stopTimer() override
    {
        return stop();
    }

    virtual bool timerIsRunning() override
    {
        return isRunning();
    }
};

bool ThreadedTimer::threadInit()
{
    m_startStamp = yarp::os::Time::now();
    return true;
}

//the newThread parameter is not in the settings for it to be unmutable and only checked by the constructor
Timer::Timer(const TimerSettings& settings, TimerCallback callback, bool newThread, Mutex* mutex) :
        //added cast for incompatible operand error
        impl(newThread ? dynamic_cast<PrivateImpl*>(new ThreadedTimer(settings, callback, mutex))
                       : dynamic_cast<PrivateImpl*>(new MonoThreadTimer(settings, callback, mutex)))
{
}

bool Timer::start()
{

    return impl->startTimer();
}

bool Timer::step()
{
    return impl->stepTimer();
}

void Timer::stop()
{
    impl->stopTimer();
}

YarpTimerEvent yarp::os::Timer::PrivateImpl::getEventNow(unsigned int iteration)
{
    YarpTimerEvent event;

    event.currentReal = yarp::os::Time::now();
    event.currentExpected = m_startStamp + iteration * m_settings.period;
    event.lastExpected = event.currentExpected - m_settings.period;
    event.lastReal = m_lastReal;
    event.lastDuration = event.currentReal - m_lastReal;
    event.runCount = iteration;
    return event;
}

bool yarp::os::Timer::PrivateImpl::runTimer(unsigned int iteration, YarpTimerEvent event)
{
    if (m_mutex) {
        m_mutex->lock();
    }

    bool ret = m_callback(event);

    if (m_mutex) {
        m_mutex->unlock();
    }

    if (!ret) {
        return false;
    }

    m_lastReal = event.currentReal;

    double timerAge = (yarp::os::Time::now() - m_startStamp);

    //totalRunCount == 0 ----> infinite run count. follows the run count of the timer
    bool stop(m_settings.totalRunCount != 0 && m_settings.totalRunCount <= iteration);

    //totalTime == 0 ----> infinite time. follows the age check for the timer
    stop |= m_settings.totalTime > 0.00001 && (m_settings.totalTime - timerAge) < m_settings.tolerance;

    if (stop) {
        return false;
    }

    return true;
}

void ThreadedTimer::run()
{
    if (getIterations() == 0 && !singleStep) {
        return;
    }
    singleStep = false;
    YarpTimerEvent event = getEventNow(this->getIterations());
    if (!runTimer(this->getIterations(), event)) {
        askToStop();
    }
}

void Timer::setSettings(const TimerSettings& settings)
{
    impl->m_settings = settings;
}

const TimerSettings Timer::getSettings()
{
    return impl->m_settings;
}

bool Timer::isRunning()
{
    return impl->timerIsRunning();
}

Timer::~Timer()
{
    delete impl;
}
