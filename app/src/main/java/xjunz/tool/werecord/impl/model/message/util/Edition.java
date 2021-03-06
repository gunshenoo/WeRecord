/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */
package xjunz.tool.werecord.impl.model.message.util;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.jetbrains.annotations.NotNull;

import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.model.message.Message;

/**
 * 消息修改的抽象类
 *
 * @author xjunz 2020/11/5 22:52
 */
public class Edition {
    //修改FLAG的数值请同时修改array/type_edition_flag的顺序
    /**
     * 消息删除标志
     */
    public static final int FLAG_REMOVAL = 3;
    /**
     * 消息替换标志
     */
    public static final int FLAG_REPLACEMENT = 1;
    /**
     * 消息插入标志
     */
    public static final int FLAG_INSERTION = 2;
    /**
     * 无编辑标志
     */
    public static final int FLAG_NONE = -1;
    /**
     * 消息编辑的指令
     */
    private final int flag;
    /**
     * 原消息id
     */
    private final long targetMsgId;

    /**
     * 原消息
     */
    private final Message victim;
    /**
     * 替换后的消息
     */
    private final Message filler;

    public int getFlag() {
        return flag;
    }

    public long getTargetMsgId() {
        return targetMsgId;
    }

    public Message getFiller() {
        return filler;
    }

    public Message getVictim() {
        return victim;
    }

    private Edition(int flag, @Nullable Message victim, @Nullable Message filler) {
        this.flag = flag;
        this.filler = filler;
        this.victim = victim;
        if (victim != null) {
            targetMsgId = victim.getMsgId();
        } else if (filler != null) {
            targetMsgId = filler.getMsgId();
        } else {
            throw new IllegalArgumentException("Neither victim nor filler can be null.");
        }
    }

    @NotNull
    public static Edition remove(Message victim) {
        return new Edition(FLAG_REMOVAL, victim, null);
    }


    @NotNull
    public static Edition replace(Message victim, Message replacement) {
        return new Edition(FLAG_REPLACEMENT, victim, replacement);
    }

    @NotNull
    public static Edition insert(Message insertion) {
        return new Edition(FLAG_INSERTION, null, insertion);
    }

    /**
     * @return 当前消息编辑指令的恢复指令
     */
    public Edition getReverseEdition() {
        switch (flag) {
            case FLAG_REMOVAL:
                return Edition.insert(victim);
            case FLAG_INSERTION:
                return Edition.remove(filler);
            case FLAG_REPLACEMENT:
                return Edition.replace(filler, victim);
        }
        throw new RuntimeException("Unexpected instruction: " + flag);
    }

    @StringRes
    public static int getEditionFlagCaptionOf(int flag) {
        switch (flag) {
            case Edition.FLAG_REMOVAL:
                return R.string.edition_type_removal;
            case Edition.FLAG_INSERTION:
                return R.string.edition_type_insertion;
            case Edition.FLAG_REPLACEMENT:
                return R.string.edition_type_rep;
            case Edition.FLAG_NONE:
                return -1;
        }
        throw new IllegalArgumentException("Unknown edition flag: " + flag);
    }


}
