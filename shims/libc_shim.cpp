#include <pthread.h>

#include <errno.h>
#include <limits.h>
#include <stdatomic.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>

#include "private/bionic_futex.h"
#include "private/bionic_time_conversions.h"

#define COND_SHARED_MASK 0x0001
#define COND_CLOCK_MASK 0x0002
#define COND_COUNTER_STEP 0x0004
#define COND_FLAGS_MASK (COND_SHARED_MASK | COND_CLOCK_MASK)

#define COND_IS_SHARED(c) (((c) & COND_SHARED_MASK) != 0)
#define COND_GET_CLOCK(c) (((c) & COND_CLOCK_MASK) >> 1)

struct pthread_cond_internal_t {
  atomic_uint state;

  bool process_shared() {
    return COND_IS_SHARED(atomic_load_explicit(&state, memory_order_relaxed));
  }

  bool use_realtime_clock() {
    return COND_GET_CLOCK(atomic_load_explicit(&state, memory_order_relaxed)) == CLOCK_REALTIME;
  }

#if defined(__LP64__)
  char __reserved[44];
#endif
};

static pthread_cond_internal_t* __get_internal_cond(pthread_cond_t* cond_interface) {
  return reinterpret_cast<pthread_cond_internal_t*>(cond_interface);
}

int pthread_cond_init(pthread_cond_t* cond_interface, const pthread_condattr_t* attr) {
  pthread_cond_internal_t* cond = __get_internal_cond(cond_interface);

  unsigned int init_state = 0;
  if (attr != nullptr) {
    init_state = (*attr & COND_FLAGS_MASK);
  }
  atomic_init(&cond->state, init_state);

  return 0;
}

static int __pthread_cond_pulse(pthread_cond_internal_t* cond, int thread_count) {
  atomic_fetch_add_explicit(&cond->state, COND_COUNTER_STEP, memory_order_relaxed);

  __futex_wake_ex(&cond->state, cond->process_shared(), thread_count);
  return 0;
}

static int __pthread_cond_timedwait(pthread_cond_internal_t* cond, pthread_mutex_t* mutex,
                                    bool use_realtime_clock, const timespec* abs_timeout_or_null) {
  int result = check_timespec(abs_timeout_or_null, true);
  if (result != 0) {
    return result;
  }

  unsigned int old_state = atomic_load_explicit(&cond->state, memory_order_relaxed);
  pthread_mutex_unlock(mutex);
  int status = __futex_wait_ex(&cond->state, cond->process_shared(), old_state,
                               use_realtime_clock, abs_timeout_or_null);
  pthread_mutex_lock(mutex);

  if (status == -ETIMEDOUT) {
    return ETIMEDOUT;
  }
  return 0;
}
