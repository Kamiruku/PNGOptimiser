include $(call all-subdir-makefiles)

include $(CLEAR_VARS)

LOCAL_MODULE 			:= turbo-jpegandroid
LOCAL_STATIC_LIBRARIES 	+= libjpeg-turbo

include $(BUILD_SHARED_LIBRARY)