/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.healthconnect.permission;

import static android.Manifest.permission.INTERACT_ACROSS_USERS_FULL;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.healthconnect.HealthPermissions;
import android.os.Binder;
import android.os.UserHandle;

import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A handler for HealthConnect permission-related logic.
 *
 * @hide
 */
public final class HealthConnectPermissionHelper {

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final Set<String> mHealthPermissions;
    private final HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    /**
     * Constructs a {@link HealthConnectPermissionHelper}.
     *
     * @param context the service context.
     * @param packageManager a {@link PackageManager} instance.
     * @param healthPermissions a {@link Set} of permissions that are recognized as
     *     HealthConnect-defined permissions.
     * @param permissionIntentTracker a {@link
     *     com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker} instance
     *     that tracks apps allowed to request health permissions.
     */
    public HealthConnectPermissionHelper(
            Context context,
            PackageManager packageManager,
            Set<String> healthPermissions,
            HealthPermissionIntentAppsTracker permissionIntentTracker) {
        mContext = context;
        mPackageManager = packageManager;
        mHealthPermissions = healthPermissions;
        mPermissionIntentAppsTracker = permissionIntentTracker;
    }

    /**
     * See {@link android.healthconnect.HealthConnectManager#grantHealthPermission}.
     *
     * <p>NOTE: Once permission grant is successful, the package name will also be appended to the
     * end of the priority list corresponding to {@code permissionName}'s health permission
     * category.
     */
    public void grantHealthPermission(
            @NonNull String packageName, @NonNull String permissionName, @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(permissionName);
        enforceManageHealthPermissions(/* message= */ "grantHealthPermission");
        enforceValidPermission(permissionName);
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        enforceSupportPermissionsUsageIntent(packageName, checkedUser);
        final long token = Binder.clearCallingIdentity();
        try {
            mPackageManager.grantRuntimePermission(packageName, permissionName, checkedUser);
            addToPriorityListIfRequired(packageName, permissionName);

        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link android.healthconnect.HealthConnectManager#revokeHealthPermission}. */
    public void revokeHealthPermission(
            @NonNull String packageName,
            @NonNull String permissionName,
            @Nullable String reason,
            @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(permissionName);
        enforceManageHealthPermissions(/* message= */ "revokeHealthPermission");
        enforceValidPermission(permissionName);
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        final long token = Binder.clearCallingIdentity();
        try {
            mPackageManager.revokeRuntimePermission(
                    packageName, permissionName, checkedUser, reason);
            removeFromPriorityListIfRequired(packageName, permissionName);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link android.healthconnect.HealthConnectManager#revokeAllHealthPermissions}. */
    public void revokeAllHealthPermissions(
            @NonNull String packageName, @Nullable String reason, @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        enforceManageHealthPermissions(/* message= */ "revokeAllHealthPermissions");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        final long token = Binder.clearCallingIdentity();
        try {
            revokeAllHealthPermissionsUnchecked(packageName, checkedUser, reason);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link android.healthconnect.HealthConnectManager#getGrantedHealthPermissions}. */
    public List<String> getGrantedHealthPermissions(
            @NonNull String packageName, @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        enforceManageHealthPermissions(/* message= */ "getGrantedHealthPermissions");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        final long token = Binder.clearCallingIdentity();
        try {
            return getGrantedHealthPermissionsUnchecked(packageName, checkedUser);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void addToPriorityListIfRequired(String packageName, String permissionName) {
        if (HealthPermissions.isWritePermission(permissionName)) {
            HealthDataCategoryPriorityHelper.getInstance()
                    .appendToPriorityList(
                            packageName, HealthPermissions.getHealthDataCategory(permissionName));
        }
    }

    private void removeFromPriorityListIfRequired(String packageName, String permissionName) {
        if (HealthPermissions.isWritePermission(permissionName)) {
            HealthDataCategoryPriorityHelper.getInstance()
                    .removeFromPriorityList(
                            packageName,
                            HealthPermissions.getHealthDataCategory(permissionName),
                            this,
                            mContext.getUser());
        }
    }

    private List<String> getGrantedHealthPermissionsUnchecked(String packageName, UserHandle user) {
        PackageInfo packageInfo;
        try {
            PackageManager packageManager =
                    mContext.createContextAsUser(user, /* flags= */ 0).getPackageManager();
            packageInfo =
                    packageManager.getPackageInfo(
                            packageName,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Invalid package", e);
        }
        List<String> grantedHealthPerms = new ArrayList<>(packageInfo.requestedPermissions.length);
        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            String currPerm = packageInfo.requestedPermissions[i];
            if (mHealthPermissions.contains(currPerm)
                    && ((packageInfo.requestedPermissionsFlags[i]
                                    & PackageInfo.REQUESTED_PERMISSION_GRANTED)
                            != 0)) {
                grantedHealthPerms.add(currPerm);
            }
        }
        return grantedHealthPerms;
    }

    private void revokeAllHealthPermissionsUnchecked(
            String packageName, UserHandle user, String reason) {
        List<String> grantedHealthPermissions =
                getGrantedHealthPermissionsUnchecked(packageName, user);
        for (String perm : grantedHealthPermissions) {
            mPackageManager.revokeRuntimePermission(packageName, perm, user, reason);
            removeFromPriorityListIfRequired(packageName, perm);
        }
    }

    private void enforceValidPermission(String permissionName) {
        if (!mHealthPermissions.contains(permissionName)) {
            throw new IllegalArgumentException("invalid health permission");
        }
    }

    private void enforceValidPackage(String packageName) {
        try {
            mPackageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("invalid package", e);
        }
    }

    private void enforceManageHealthPermissions(String message) {
        mContext.enforceCallingOrSelfPermission(
                HealthPermissions.MANAGE_HEALTH_PERMISSIONS, message);
    }

    private void enforceSupportPermissionsUsageIntent(String packageName, UserHandle userHandle) {
        if (!mPermissionIntentAppsTracker.supportsPermissionUsageIntent(packageName, userHandle)) {
            throw new SecurityException(
                    "Package "
                            + packageName
                            + " for "
                            + userHandle.toString()
                            + " doesn't support health permissions usage intent.");
        }
    }

    /**
     * Returns the target userId after handling the incoming user for packages with {@link
     * INTERACT_ACROSS_USERS_FULL}.
     *
     * @throws java.lang.SecurityException if the caller is affecting different users without
     *     holding the {@link INTERACT_ACROSS_USERS_FULL} permission.
     */
    private int handleIncomingUser(int userId) {
        int callingUserId = UserHandle.getUserHandleForUid(Binder.getCallingUid()).getIdentifier();
        if (userId == callingUserId) {
            return userId;
        }

        boolean canInteractAcrossUsersFull =
                mContext.checkCallingPermission(INTERACT_ACROSS_USERS_FULL)
                        == PackageManager.PERMISSION_GRANTED;
        if (canInteractAcrossUsersFull) {
            if (userId == UserHandle.CURRENT.getIdentifier()) {
                return ActivityManager.getCurrentUser();
            }
            return userId;
        }

        throw new SecurityException(
                "Permission denied. Need to run as either the calling user id ("
                        + callingUserId
                        + "), or with "
                        + INTERACT_ACROSS_USERS_FULL
                        + " permission");
    }
}
