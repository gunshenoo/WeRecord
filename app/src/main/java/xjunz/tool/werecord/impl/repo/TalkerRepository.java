/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl.repo;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.werecord.impl.model.account.Contact;
import xjunz.tool.werecord.impl.model.account.Talker;

public class TalkerRepository extends AccountRepository<Talker> {
    private static final int CACHE_CAPACITY = 50;
    private final SimpleArrayMap<Contact.Type, List<Talker>> mMap = new SimpleArrayMap<>();
    public static final String TABLE_CONVERSATION = "rconversation";

    TalkerRepository() {
    }

    @Override
    public int getCacheCapacity() {
        return CACHE_CAPACITY;
    }

    public void remove(Talker talker) {
        mAll.remove(talker);
    }

    /**
     * 查询所有聊天数据
     */
    @Override
    protected void queryAllInternal(@NonNull List<Talker> all) {
        SQLiteDatabase database = getDatabase();
        Cursor talkerQueryCursor = database.rawQuery("select username,conversationTime,msgCount,parentRef,unreadCount from rconversation where not msgCount = 0", null);
        while (talkerQueryCursor.moveToNext()) {
            String id = talkerQueryCursor.getString(0);
            if (!TextUtils.isEmpty(id)) {
                Talker talker = new Talker(id);
                Cursor contactQueryCursor = database.rawQuery("select alias,conRemark,nickname,type from rcontact where username='" + id + "'", null);
                if (contactQueryCursor.moveToNext()) {
                    talker.alias = contactQueryCursor.getString(0);
                    talker.remark = contactQueryCursor.getString(1);
                    talker.nickname = contactQueryCursor.getString(2);
                    talker.rawType = contactQueryCursor.getInt(3);
                    talker.judgeType();
                }
                contactQueryCursor.close();
                talker.lastMsgTimestamp = talkerQueryCursor.getLong(1);
                talker.messageCount = talkerQueryCursor.getInt(2);
                talker.parentRef = talkerQueryCursor.getString(3);
                talker.setUnreadCount(talkerQueryCursor.getInt(4));
                getAllOfType(talker.type).add(talker);
                all.add(talker);
            }
        }
        talkerQueryCursor.close();
    }

    /**
     * 我们没有这个需求，因为所有{@link Talker}都会在应用初始化时加载
     */
    @Override
    protected Talker query(String id) {
        throw new IllegalArgumentException("This method is not expected to be called!\n When this exception occurred, " +
                "it means you are calling get(String id) with an id (here it is " + id + ") that doesn't exist in the database. " +
                "Pls check the id you passed in. ");
    }


    @NonNull
    public List<Talker> getAllOfType(Contact.Type type) {
        List<Talker> accounts = mMap.get(type);
        if (accounts == null) {
            accounts = new ArrayList<>();
            mMap.put(type, accounts);
        }
        return accounts;
    }
}
