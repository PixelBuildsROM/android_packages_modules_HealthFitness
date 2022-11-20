/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.deletion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants.CONFIRMATION_EVENT
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_TYPE
import com.android.healthconnect.controller.deletion.DeletionConstants.GO_BACK_EVENT
import com.android.healthconnect.controller.deletion.DeletionConstants.START_DELETION_EVENT
import com.android.healthconnect.controller.deletion.DeletionConstants.TIME_RANGE_SELECTION_EVENT
import com.android.healthconnect.controller.deletion.DeletionConstants.TRY_AGAIN_EVENT
import dagger.hilt.android.AndroidEntryPoint

/**
 * Invisible fragment that handles every deletion flow with the deletion dialogs.
 *
 * <p>This fragment needs to be added to every page that performs deletion. Then the deletion flow
 * can be started via {@link StartDeletionEvent}.
 *
 * <p>It can be added to the parent fragment without attaching to a view via the following snippet:
 *
 * <pre> if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
 * ```
 *      childFragmentManager
 *          .commitNow {
 *              add({@link DeletionFragment}(), FRAGMENT_TAG_DELETION)
 *          }
 * ```
 * } </pre>
 */
@AndroidEntryPoint(Fragment::class)
class DeletionFragment : Hilt_DeletionFragment() {

    private val viewModel: DeletionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set event listeners
        // start deletion
        parentFragmentManager.setFragmentResultListener(START_DELETION_EVENT, this) { _, bundle ->
            val deletionType = bundle.getParcelable(DELETION_TYPE) as DeletionType?
            viewModel.setDeletionType(deletionType!!)
            showFirstDialog()
        }

        // time range selection
        childFragmentManager.setFragmentResultListener(TIME_RANGE_SELECTION_EVENT, this) { _, _ ->
            showConfirmationDialog()
        }

        // confirmation dialog
        childFragmentManager.setFragmentResultListener(GO_BACK_EVENT, this) { _, _ ->
            showTimeRagePickerDialog()
        }

        // deletion in progress
        childFragmentManager.setFragmentResultListener(CONFIRMATION_EVENT, this) { _, _ ->
            // start deletion from here which will trigger the progressDialog from observable
            viewModel.delete()
        }

        // try again
        childFragmentManager.setFragmentResultListener(TRY_AGAIN_EVENT, this) { _, _ ->
            showConfirmationDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deletion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.deletionParameters.observe(viewLifecycleOwner) { deletion ->
            deletion?.let { render(deletion) }
        }
    }

    private fun render(deletionParameters: DeletionParameters) {
        when (deletionParameters.deletionState) {
            DeletionState.STATE_NO_DELETION_IN_PROGRESS -> {
                // clear dialogs?
                hideProgressDialog()
            }
            DeletionState.STATE_PROGRESS_INDICATOR_STARTED -> {
                showProgressDialogFragment()
            }
            DeletionState.STATE_PROGRESS_INDICATOR_CAN_END -> {
                hideProgressDialog()
            }
            DeletionState.STATE_DELETION_SUCCESSFUL -> {
                showSuccessDialogFragment()
            }
            DeletionState.STATE_DELETION_FAILED -> {
                showFailedDialogFragment()
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun showConfirmationDialog() {
        DeletionConfirmationDialogFragment()
            .show(childFragmentManager, DeletionConfirmationDialogFragment.TAG)
    }

    private fun showTimeRagePickerDialog() {
        TimeRangeDialogFragment().show(childFragmentManager, TimeRangeDialogFragment.TAG)
    }

    private fun showProgressDialogFragment() {
        ProgressDialogFragment().show(childFragmentManager, ProgressDialogFragment.TAG)
    }

    private fun showSuccessDialogFragment() {
        hideProgressDialog()
        SuccessDialogFragment().show(childFragmentManager, SuccessDialogFragment.TAG)
    }

    private fun showFailedDialogFragment() {
        hideProgressDialog()
        FailedDialogFragment().show(childFragmentManager, FailedDialogFragment.TAG)
    }

    private fun showFirstDialog(showPicker: Boolean = true) {
        if (showPicker) {
            showTimeRagePickerDialog()
        }

        // TODO implement other flows which do not require TimeRangePicker
    }

    private fun hideProgressDialog() {
        (childFragmentManager.findFragmentByTag(ProgressDialogFragment.TAG)
                as ProgressDialogFragment?)
            ?.dismiss()
    }
}
