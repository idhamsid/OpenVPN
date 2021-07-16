package com.zboot.vpn.vpn

import android.app.AlertDialog
import android.content.DialogInterface
import android.text.InputType
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.zboot.vpn.R
import de.blinkt.openvpn.VPNLauncher
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus


class VPNLauncherImpl(
		private val fragment: Fragment,
		callback: VPNLauncherCallbacks,
		serviceClass: Class<VPNService>
) : VPNLauncher<VPNService>(fragment, callback, serviceClass) {

	//TODO personalize later
	override fun askForPW(type: Int) {
		val entry = EditText(fragment.requireContext())

		entry.setSingleLine()
		entry.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
		entry.transformationMethod = PasswordTransformationMethod()

		val dialog = AlertDialog.Builder(fragment.requireContext())
		dialog.setTitle(fragment.getString(R.string.pw_request_dialog_title, fragment.getString(type)))
		dialog.setMessage(fragment.getString(R.string.pw_request_dialog_prompt, mSelectedProfile.mName))


		val userPasswordLayout: View = fragment.layoutInflater.inflate(R.layout.userpass_layout, null, false)

		val usernameET = userPasswordLayout.findViewById<EditText>(R.id.username)
		val passwordET = userPasswordLayout.findViewById<EditText>(R.id.password)
		val savePasswordET = userPasswordLayout.findViewById<CheckBox>(R.id.save_password)
		val showPasswordET = userPasswordLayout.findViewById<CheckBox>(R.id.show_password)

		if (type == R.string.password) {
			usernameET.setText(mSelectedProfile.mUsername)
			passwordET.setText(mSelectedProfile.mPassword)
			savePasswordET.isChecked = !TextUtils.isEmpty(mSelectedProfile.mPassword)

			showPasswordET.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
				if (isChecked)
					passwordET.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
				else
					passwordET.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
			}
			dialog.setView(userPasswordLayout)
		} else {
			dialog.setView(entry)
		}

		dialog.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
			if (type == R.string.password) {
				mSelectedProfile.mUsername = usernameET.text.toString()
				val pw = passwordET.text.toString()
				if (savePasswordET.isChecked) {
					mSelectedProfile.mPassword = pw
				} else {
					mSelectedProfile.mPassword = null
					mTransientAuthPW = pw
				}
			} else {
				mTransientCertOrPCKS12PW = entry.text.toString()
			}
		}

		dialog.setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
			VpnStatus.updateStateString(
					"USER_VPN_PASSWORD_CANCELLED",
					"",
					R.string.state_user_vpn_password_cancelled,
					ConnectionStatus.LEVEL_DISCONNECTED
			)
		}

		dialog.show()
	}
}