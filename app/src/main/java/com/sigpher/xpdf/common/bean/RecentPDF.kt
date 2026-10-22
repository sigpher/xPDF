package com.sigpher.xpdf.common.bean

import android.os.Parcel
import android.os.Parcelable
import org.greenrobot.greendao.annotation.Entity
import org.greenrobot.greendao.annotation.Generated
import org.greenrobot.greendao.annotation.Id
import org.greenrobot.greendao.annotation.Unique

/**
 * 最近阅读实体（greenDAO 表 RECENT_PDF），只记录 dir + name 两个关联键。
 *
 * 属性名即列名，不得改名；字符串保持可空以兼容历史数据。
 *
 * @author sigpher
 */
@Entity
class RecentPDF() : Parcelable {

    @field:Id(autoincrement = true)
    var id: Long? = null
    var dir: String? = null

    @field:Unique
    var name: String? = null

    constructor(dir: String?, name: String?) : this() {
        this.dir = dir
        this.name = name
    }

    /**
     * greenDAO 生成的 DAO 用该全参构造读取一条记录（字段顺序与表列顺序一致）
     */
    @Generated(hash = 705905111)
    constructor(id: Long?, dir: String?, name: String?) : this() {
        this.id = id
        this.dir = dir
        this.name = name
    }

    protected constructor(parcel: Parcel) : this() {
        this.id = parcel.readValue(Long::class.java.classLoader) as Long?
        this.dir = parcel.readString()
        this.name = parcel.readString()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeValue(this.id)
        dest.writeString(this.dir)
        dest.writeString(this.name)
    }

    override fun toString(): String {
        return "RecentPDF{" +
                "id=" + id +
                ", dir='" + dir + '\'' +
                ", name='" + name + '\'' +
                '}'
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<RecentPDF> = object : Parcelable.Creator<RecentPDF> {
            override fun createFromParcel(source: Parcel): RecentPDF = RecentPDF(source)

            override fun newArray(size: Int): Array<RecentPDF?> = arrayOfNulls(size)
        }
    }
}
