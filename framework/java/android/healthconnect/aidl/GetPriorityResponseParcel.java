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

package android.healthconnect.aidl;

import android.healthconnect.GetPriorityResponse;
import android.healthconnect.datatypes.DataOrigin;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.stream.Collectors;

/** @hide */
public final class GetPriorityResponseParcel implements Parcelable {
    public static final Creator<GetPriorityResponseParcel> CREATOR =
            new Creator<GetPriorityResponseParcel>() {
                @Override
                public GetPriorityResponseParcel createFromParcel(Parcel in) {
                    return new GetPriorityResponseParcel(in);
                }

                @Override
                public GetPriorityResponseParcel[] newArray(int size) {
                    return new GetPriorityResponseParcel[size];
                }
            };
    private final List<String> mPackagesInPriorityOrder;

    private GetPriorityResponseParcel(Parcel in) {
        mPackagesInPriorityOrder = in.createStringArrayList();
    }

    public GetPriorityResponseParcel(@NonNull GetPriorityResponse getPriorityResponse) {
        mPackagesInPriorityOrder =
                getPriorityResponse.getDataOriginInPriority().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mPackagesInPriorityOrder);
    }
}
