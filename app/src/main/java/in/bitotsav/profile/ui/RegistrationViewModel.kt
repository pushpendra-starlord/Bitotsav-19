package `in`.bitotsav.profile.ui

import `in`.bitotsav.notification.utils.sendFcmTokenToServer
import `in`.bitotsav.profile.data.RegistrationFields.college
import `in`.bitotsav.profile.data.RegistrationFields.collegeOptions
import `in`.bitotsav.profile.data.RegistrationFields.email
import `in`.bitotsav.profile.data.RegistrationFields.emailOtp
import `in`.bitotsav.profile.data.RegistrationFields.gender
import `in`.bitotsav.profile.data.RegistrationFields.name
import `in`.bitotsav.profile.data.RegistrationFields.password
import `in`.bitotsav.profile.data.RegistrationFields.phone
import `in`.bitotsav.profile.data.RegistrationFields.phoneOtp
import `in`.bitotsav.profile.data.RegistrationFields.rollNo
import `in`.bitotsav.profile.data.RegistrationFields.source
import `in`.bitotsav.profile.data.RegistrationFields.year
import `in`.bitotsav.profile.data.RegistrationFields.yearOptions
import `in`.bitotsav.profile.utils.*
import `in`.bitotsav.shared.exceptions.NetworkException
import `in`.bitotsav.shared.ui.BaseViewModel
import `in`.bitotsav.shared.utils.onFalse
import `in`.bitotsav.shared.utils.onTrue
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegistrationViewModel : BaseViewModel("RegVM") {

    val currentStep = NonNullMutableLiveData(1)
    val registrationError = NonNullMutableLiveData("")
    val waiting = NonNullMutableLiveData(false)
    val allDone = NonNullMutableLiveData(false)
    val loggedIn = NonNullMutableLiveData(false)

    fun fetchCollegeList() {
        scope.launch(Dispatchers.IO)
        {
            try {
                collegeOptions.postValue(fetchCollegeListAsync().await())
                Log.v(TAG, "College options fetched")
            } catch (e: Exception) {
                Log.w(TAG, e.message, e)
            }
        }
    }

    // Common
    private val anyErrors
        get() = when (currentStep.value) {
            2 -> listOf(phoneOtp, emailOtp)
            3 -> listOf(gender, college, rollNo, source)
            else -> listOf(name, phone, email, password)
        }
            .any {
                it.errorText.value.isNotEmpty().onTrue {
                    Log.v(TAG, "Step ${currentStep.value}: ${it.text}: ${it.errorText}")
                }
            }.onFalse {
                Log.v(TAG, "All validations succeeded for step ${currentStep.value}")
            }

    fun completeStepOne(recaptchaResponseToken: String) {
        scope.launch {
            try {

                registerAsync(
                    name.text.value,
                    phone.text.value,
                    email.text.value,
                    password.text.value,
                    recaptchaResponseToken
                ).await()
                currentStep.value = 2

            } catch (e: NetworkException) {
                registrationError.value = e.message ?: "Network Error"
                Log.e(TAG, null, e)
            } catch (e: AuthException) {
                registrationError.value = e.message ?: "Authentication Error"
                Log.e(TAG, null, e)
            } catch (e: Exception) {
                registrationError.value = "Unknown Error :("
                Log.e(TAG, null, e)
            } finally {
                waiting.value = false
            }
        }
    }

    fun completeStepTwo() {
        scope.launch {
            try {

                verifyAsync(
                    email.text.value,
                    phoneOtp.text.value,
                    emailOtp.text.value
                ).await()
                currentStep.value = 3

            } catch (e: NetworkException) {
                registrationError.value = e.message ?: "Network Error"
                Log.e(TAG, null, e)
            } catch (e: AuthException) {
                registrationError.value = e.message ?: "Authentication Error"
                Log.e(TAG, null, e)
            } catch (e: Exception) {
                registrationError.value = "Unknown Error :("
                Log.e(TAG, null, e)
            } finally {
                waiting.value = false
            }
        }
    }

    fun completeStepThree() {
        scope.launch {
            try {

                saveParticipantAsync(
                    email.text.value,
//                    TODO: @aksh Check if this can somehow break the operation. Note: Password value is required.
                    password.text.value,
                    gender.text.value,
                    college.text.value,
                    rollNo.text.value,
                    source.text.value,
                    yearOptions.indexOf(year.text.value) + 1
                ).await()
                allDone.value = true

            } catch (e: NetworkException) {
                registrationError.value = e.message ?: "Network Error"
                Log.e(TAG, null, e)
            } catch (e: AuthException) {
                registrationError.value = e.message ?: "Authentication Error"
                Log.e(TAG, null, e)
            } catch (e: Exception) {
                registrationError.value = "Unknown Error :("
                Log.e(TAG, null, e)
            } finally {
                waiting.value = false
            }
        }
    }

    fun login() {
        scope.launch {
            try {

                loginAsync(email.text.value, password.text.value).await()
                syncUserAndRun { sendFcmTokenToServer() }
                loggedIn.value = true

            } catch (e: Exception) {
                Log.e("$TAG::login", "Unable to auto-login after registration", e)
            } finally {

                waiting.value = false
                currentStep.value = 1

            }
        }
    }

    fun next() {
        Log.v(TAG, "Attempting step ${currentStep.value}")
        if (anyErrors) return
        registrationError.value = ""
        waiting.value = true
    }
}
