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

package com.android.server.healthconnect.storage.datatypehelpers;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.IntermenstrualBleedingRecordInternal;
import android.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for {@link android.healthconnect.datatypes.IntermenstrualBleedingRecord}.
 *
 * @hide
 */
@HelperFor(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING)
public final class IntermenstrualBleedingRecordHelper
        extends InstantRecordHelper<IntermenstrualBleedingRecordInternal> {
    private static final String INTERMENSTRUAL_BLEEDING_RECORD_TABLE_NAME =
            "intermenstrual_bleeding_record_table";

    @Override
    @NonNull
    public String getMainTableName() {
        return INTERMENSTRUAL_BLEEDING_RECORD_TABLE_NAME;
    }

    @Override
    void populateSpecificContentValues(
            @NonNull ContentValues contentValues,
            @NonNull IntermenstrualBleedingRecordInternal intermenstrualBleedingRecordInternal) {}

    @Override
    protected void populateSpecificRecordValue(
            @NonNull Cursor cursor, @NonNull IntermenstrualBleedingRecordInternal recordInternal) {}

    @Override
    @NonNull
    protected List<Pair<String, String>> getInstantRecordColumnInfo() {
        return Collections.emptyList();
    }
}