package com.sigpher.xpdf.common.bean

import android.os.Parcel
import android.os.Parcelable
import org.greenrobot.greendao.annotation.Entity
import org.greenrobot.greendao.annotation.Generated
import org.greenrobot.greendao.annotation.Id
import org.greenrobot.greendao.annotation.Unique

/**
 * 分组实体（greenDAO 表 COLLECTION）。
 *
 * 属性名即列名，且 `Backup` 备份由 Gson 按字段名读写，不得改名；字符串保持可空以兼容历史数据。
 *
 * @author sigpher
 */
@Entity
class Collection() : Parcelable {

    @field:Id(autoincrement = true)
    var id: Long? = null

    @field:Unique
    var name: String? = null
    var position: Int = 0

    constructor(name: String?, position: Int) : this() {
        this.name = name
        this.position = position
    }

    /**
     * greenDAO 生成的 DAO 用该全参构造读取一条记录（字段顺序与表列顺序一致）
     */
    @Generated(hash = 100999361)
    constructor(id: Long?, name: String?, position: Int) : this() {
        this.id = id
        this.name = name
        this.position = position
    }

    protected constructor(parcel: Parcel) : this() {
        this.id = parcel.readValue(Long::class.java.classLoader) as Long?
        this.name = parcel.readString()
        this.position = parcel.readInt()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeValue(this.id)
        dest.writeString(this.name)
        dest.writeInt(this.position)
    }

    override fun toString(): String {
        return "Collection{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", position=" + position +
                '}'
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<Collection> = object : Parcelable.Creator<Collection> {
            override fun createFromParcel(source: Parcel): Collection = Collection(source)

            override fun newArray(size: Int): Array<Collection?> = arrayOfNulls(size)
        }
    }
}
