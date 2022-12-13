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

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorIntegerList;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.AccessLog;
import android.healthconnect.Constants;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A helper class to fetch and store the access logs.
 *
 * @hide
 */
public final class AccessLogsHelper {
    public static final String TABLE_NAME = "access_logs_table";
    private static final String RECORD_TYPE_COLUMN_NAME = "record_type";
    private static final String APP_ID_COLUMN_NAME = "app_id";
    private static final String ACCESS_TIME_COLUMN_NAME = "access_time";
    private static final String OPERATION_TYPE_COLUMN_NAME = "operation_type";
    private static final int NUM_COLS = 5;
    private static AccessLogsHelper sAccessLogsHelper;

    private AccessLogsHelper() {}

    public static AccessLogsHelper getInstance() {
        if (sAccessLogsHelper == null) {
            sAccessLogsHelper = new AccessLogsHelper();
        }

        return sAccessLogsHelper;
    }

    @NonNull
    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    /**
     * @return AccessLog list
     */
    public List<AccessLog> queryAccessLogs() {
        final ReadTableRequest readTableRequest = new ReadTableRequest(TABLE_NAME);

        List<AccessLog> accessLogsList = new ArrayList<>();
        AppInfoHelper appInfoHelper = AppInfoHelper.getInstance();
        try (SQLiteDatabase db = TransactionManager.getInitialisedInstance().getReadableDb();
                Cursor cursor =
                        TransactionManager.getInitialisedInstance().read(db, readTableRequest)) {
            while (cursor.moveToNext()) {
                String packageName =
                        String.valueOf(
                                appInfoHelper.getPackageName(
                                        getCursorLong(cursor, APP_ID_COLUMN_NAME)));
                @RecordTypeIdentifier.RecordType
                List<Integer> recordTypes =
                        getCursorIntegerList(cursor, RECORD_TYPE_COLUMN_NAME, DELIMITER);
                long accessTime = getCursorLong(cursor, ACCESS_TIME_COLUMN_NAME);
                @Constants.OperationType
                int operationType = getCursorInt(cursor, OPERATION_TYPE_COLUMN_NAME);
                accessLogsList.add(
                        new AccessLog(packageName, recordTypes, accessTime, operationType));
            }
        }

        return accessLogsList;
    }

    /** Adds an entry in to the access logs table for every insert or read operation request */
    public void addAccessLog(
            String packageName,
            @RecordTypeIdentifier.RecordType List<Integer> recordTypeList,
            @Constants.OperationType int operationType) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(
                RECORD_TYPE_COLUMN_NAME,
                recordTypeList.stream().map(String::valueOf).collect(Collectors.joining(",")));
        contentValues.put(
                APP_ID_COLUMN_NAME, AppInfoHelper.getInstance().getAppInfoId(packageName));
        contentValues.put(ACCESS_TIME_COLUMN_NAME, Instant.now().toEpochMilli());
        contentValues.put(OPERATION_TYPE_COLUMN_NAME, operationType);

        UpsertTableRequest request = new UpsertTableRequest(TABLE_NAME, contentValues);
        TransactionManager.getInitialisedInstance().insert(request);
    }

    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        List<Pair<String, String>> columnInfo = new ArrayList<>(NUM_COLS);
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(APP_ID_COLUMN_NAME, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(RECORD_TYPE_COLUMN_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(ACCESS_TIME_COLUMN_NAME, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(OPERATION_TYPE_COLUMN_NAME, INTEGER_NOT_NULL));

        return columnInfo;
    }
}