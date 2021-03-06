/*
 * Copyright (c) 2021 xjunz. 保留所有权利
 */

package xjunz.tool.werecord.impl.model.account;

import android.content.ContentValues;
import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xjunz.tool.werecord.App;
import xjunz.tool.werecord.R;
import xjunz.tool.werecord.impl.model.export.TableExportable;
import xjunz.tool.werecord.impl.model.message.util.LvBufferUtils;
import xjunz.tool.werecord.impl.repo.ContactRepository;
import xjunz.tool.werecord.ui.viewmodel.SortBy;
import xjunz.tool.werecord.util.LogUtils;
import xjunz.tool.werecord.util.Utils;

public class Contact extends Account implements TableExportable {
    /**
     * 为此联系人设置的备注
     */
    public String remark;
    /**
     * 从数据库查询得到的type字段
     */
    public int rawType;
    /**
     * 当前{@link Contact}的精确类型，适用于所有联系人
     *
     * @see Contact#judgeType()
     */
    public Type type;
    /**
     * rawType: 服务号
     */
    protected static final int RAW_TYPE_SERVICE = 33;
    /**
     * rawType: 陌生人
     */
    protected static final int RAW_TYPE_UNSAVED_FRIEND = 4;
    /**
     * rawType: 1或3是保存的公众号，好友，群聊；暂时不知道区别
     */
    @Keep
    protected static final int RAW_TYPE_SAVED_3 = 3;
    @Keep
    protected static final int RAW_TYPE_SAVED_1 = 1;
    /**
     * rawType: 我方删除的好友，公众号，群聊
     */
    protected static final int RAW_TYPE_DELETED = 0;
    /**
     * rawType: 未保存的群聊
     */
    protected static final int RAW_TYPE_UNSAVED_GROUP = 2;
    /**
     * rawType: 不看朋友圈的朋友
     */
    protected static final int RAW_TYPE_BLOCK_PYQ = 0x10003;
    /**
     * 处理得到的用于排序的名称拼音缩写
     *
     * @see Contact#getComparatorPyAbbr()
     */
    private String comparatorPyAbbr;
    /**
     * 名称的拼音缩写
     *
     * @see Contact#getNamePyAttr()
     */
    private String pyAbbr;

    private byte[] lvBuffer;
    public static final int[] LV_BUFFER_READ_SERIAL = {1, 1, 0, 3, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 3, 3};
    private static final Object[] EMPTY_LV_BUFFER = LvBufferUtils.createEmptyLvBuffer(LV_BUFFER_READ_SERIAL);
    private Object[] parsedLvBuffer = EMPTY_LV_BUFFER;

    public Contact(String id) {
        this.id = id;
    }

    public byte[] getLvBuffer() {
        return lvBuffer;
    }

    public void setLvBuffer(byte[] lvBuffer) {
        this.lvBuffer = lvBuffer;
        LvBufferUtils utils = new LvBufferUtils();
        try {
            parsedLvBuffer = utils.readLvBuffer(lvBuffer, LV_BUFFER_READ_SERIAL);
        } catch (Exception e) {
            LogUtils.error("Failed to parse LvBuffer, id: " + id);
        }
    }

    public Object[] getParsedLvBuffer() {
        return parsedLvBuffer;
    }

    /**
     * 返回某个好友是否为单向好友（僵尸）
     */
    public boolean isPossibleZombie() {
        if (type == Type.FRIEND && parsedLvBuffer != null) {
            String encrypted = (String) parsedLvBuffer[32];
            return encrypted != null && encrypted.endsWith("@stranger");
        }
        return false;
    }

    /**
     * 获取用于排序的名称拼音缩写，名称的优先级为备注、昵称、微信号、微信ID。
     * 首字母ASCII码小于‘0’或者大于‘9’且小于‘A’的字符前会加上"#",代表符号开头的名称，
     * 大于‘z’的字符前会加上"?",代表其他类型的字符开头的名称（非符号、数字、字母），
     * 这样它们就会被分类在一起进行排序。
     *
     * @return 用于排序的名称拼音缩写
     */
    protected String getComparatorPyAbbr() {
        if (comparatorPyAbbr == null) {
            comparatorPyAbbr = getNamePyAttr();
            char first = comparatorPyAbbr.charAt(0);
            if (first < '0' || (first > '9' && first < 'A') || (first > 'Z' && first < 'a')) {
                comparatorPyAbbr = "#" + comparatorPyAbbr;
            } else if (first > 'z') {
                comparatorPyAbbr = "?" + comparatorPyAbbr;
            }
            comparatorPyAbbr = comparatorPyAbbr.toUpperCase();
        }
        return comparatorPyAbbr;
    }

    public String getNamePyAttr() {
        if (pyAbbr == null) {
            pyAbbr = Utils.getPinYinAbbr(getName());
        }
        return pyAbbr;
    }

    public int compareTo(@NonNull Contact o, SortBy by, boolean isAscending) {
        return (isAscending ? 1 : -1) * getComparatorPyAbbr().compareTo(o.getComparatorPyAbbr());
    }

    /**
     * @return 0: 无/未知 1：男 2：女
     */
    public int getGender() {
        return (int) parsedLvBuffer[1];
    }

    public String getRegion() {
        String provinceOrCountry = (String) parsedLvBuffer[14];
        if (!empty(provinceOrCountry)) {
            String city = (String) parsedLvBuffer[15];
            String enRegion = (String) parsedLvBuffer[22];
            if (empty(city)) {
                return String.format("%s (%s)", provinceOrCountry, enRegion);
            } else {
                return String.format("%s %s (%s)", provinceOrCountry, city, enRegion);
            }
        } else {
            return null;
        }
    }

    public String getBio() {
        return (String) parsedLvBuffer[13];
    }

    public String getPhoneNumbers() {
        String phoneNumbers = (String) parsedLvBuffer[31];
        if (phoneNumbers != null && phoneNumbers.endsWith("，")) {
            phoneNumbers = phoneNumbers.substring(0, phoneNumbers.length() - 1);
        }
        return phoneNumbers;
    }

    /**
     * [1]
     * 类型：好友
     * 昵称：XXX
     * 备注：XXX
     * 微信ID：wxid_xxx
     * 微信号：XXX
     * 性别：女
     * 地区：XX省 XX市 (XXX XXX)
     * 签名：XXX
     * 电话号码：12345678910
     */
    @Override
    public String exportAsPlainText() {
        StringBuilder builder = new StringBuilder();
        appendIfNotEmpty(builder, "类型", type.caption);
        appendIfNotEmpty(builder, "昵称", nickname);
        appendIfNotEmpty(builder, "备注", remark);
        appendIfNotEmpty(builder, "微信ID", id);
        appendIfNotEmpty(builder, "微信号", alias);
        int gender = getGender();
        if (gender != 0) {
            appendIfNotEmpty(builder, "性别", gender == 1 ? "男" : "女");
        }
        appendIfNotEmpty(builder, "地区", getRegion());
        appendIfNotEmpty(builder, "个性签名", getBio());
        appendIfNotEmpty(builder, "电话号码", getPhoneNumbers());
        return builder.toString();
    }

    private void appendIfNotEmpty(@NotNull StringBuilder builder, String name, String value) {
        if (!empty(value)) {
            builder.append(name).append(": ").append(value).append("\n");
        }
    }

    @Override
    public ContentValues exportAsContentValues() {
        return null;
    }

    @Override
    public String exportAsHtml() {
        return null;
    }

    @Override
    public String exportAsTableElement(long ordinal) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>").append("\n");
        td(sb, String.valueOf(ordinal));
        td(sb, nickname);
        td(sb, id);
        td(sb, alias);
        td(sb, remark);
        td(sb, getGender() == 0 ? null : getGender() == 1 ? "男" : "女");
        td(sb, getRegion());
        td(sb, getBio());
        td(sb, getPhoneNumbers());
        sb.append("</tr>");
        return sb.toString();
    }

    /**
     * 处理后的Contact类型枚举类
     * <br/>规则句法:[{@link Contact#rawType}等于XX]([逻辑运算符][{@link Contact#id}满足的条件])
     */
    public enum Type {
        /**
         * 好友: （1||3||65539）&& !endsWith("@chatroom")&&!startWith("gh_")
         */
        FRIEND(R.string.type_friend),
        /**
         * 删除的好友: 0 && !endsWith("@chatroom")&&!startWith("gh_")
         */
        DELETED_FRIEND(R.string.type_deleted_friend),
        /**
         * 陌生人: 4
         */
        STRANGER(R.string.type_stranger),
        /**
         * 加入的群聊:2||（ 3 && endsWith("_chatroom")）
         */
        JOINED_GROUP(R.string.type_joined_group),
        /**
         * 退出的群聊: 0 && endsWith("chatroom")
         */
        QUITED_GROUP(R.string.type_quited_group),
        /**
         * 关注的公众号: （1||3）&& startsWith("gh_")
         */
        FOLLOWING_GZH(R.string.type_following_gzh),
        /**
         * 取消关注的公众号: 0 && startsWith("gh_")
         */
        UNFOLLOWED_GZH(R.string.type_unfollowed_gzh),
        /**
         * 微信官方账号: 33
         */
        SERVICE(R.string.type_service);
        /**
         * 类型的名称资源ID
         */
        public String caption;


        Type(int captionRes) {
            this.caption = App.getStringOf(captionRes);
        }

        @NotNull
        public static List<String> getCaptionList(@NotNull Type... types) {
            List<String> captions = new ArrayList<>();
            for (Type type : types) {
                captions.add(type.caption);
            }
            return captions;
        }
    }

    /**
     * 获取某特定排序规则下的描述，描述的用途在于为数据分类,相同描述的数据可视为一类，方便区分和筛选。
     * 例如，当排序规则为{@link SortBy#NAME}时，即按名称排序时，此时的描述为当前联系人名称的拼音缩写首字母。
     *
     * @param sortBy 指定排序规则
     * @return 返回当前对象的描述
     */
    @NonNull
    public String describe(SortBy sortBy) {
        if (sortBy == SortBy.NAME) {
            return getComparatorPyAbbr().substring(0, 1);
        }
        throw new IllegalArgumentException("Contact can be described with NAME only! ");
    }


    /**
     * 获取当前{@code Contact}对象的类型，主要根据{@link Contact#rawType}进行判断，直观的判断规则见{@link Type}的注释
     * <br/>此方法应当被{@link ContactRepository#queryAll()}中被调用，获取当前对象的类型直接访问{@link Contact#type}即可
     */
    public void judgeType() {
        switch (rawType) {
            case RAW_TYPE_SERVICE:
                this.type = Type.SERVICE;
                break;
            case RAW_TYPE_DELETED:
                if (isGZH()) {
                    this.type = Type.UNFOLLOWED_GZH;
                } else if (isGroup()) {
                    this.type = Type.QUITED_GROUP;
                } else {
                    this.type = Type.DELETED_FRIEND;
                }
                break;
            case RAW_TYPE_UNSAVED_FRIEND:
                this.type = Type.STRANGER;
                break;
            case RAW_TYPE_UNSAVED_GROUP:
                this.type = Type.JOINED_GROUP;
                break;
            default:
                if (isGZH()) {
                    this.type = Type.FOLLOWING_GZH;
                } else if (isGroup()) {
                    this.type = Type.JOINED_GROUP;
                } else {
                    this.type = Type.FRIEND;
                }
                break;
        }
    }


    @Override
    public String getName() {
        return TextUtils.isEmpty(remark) ? super.getName() : remark;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.remark);
        dest.writeInt(this.rawType);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeString(this.comparatorPyAbbr);
        dest.writeString(this.pyAbbr);
        dest.writeByteArray(this.lvBuffer);
        dest.writeArray(this.parsedLvBuffer);
    }

    protected Contact(Parcel in) {
        super(in);
        this.remark = in.readString();
        this.rawType = in.readInt();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : Type.values()[tmpType];
        this.comparatorPyAbbr = in.readString();
        this.pyAbbr = in.readString();
        this.lvBuffer = in.createByteArray();
        this.parsedLvBuffer = in.readArray(Object[].class.getClassLoader());
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
