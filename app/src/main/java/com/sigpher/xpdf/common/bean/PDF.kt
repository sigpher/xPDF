package com.sigpher.xpdf.common.bean

import android.os.Parcel
import android.os.Parcelable
import org.greenrobot.greendao.annotation.Entity
import org.greenrobot.greendao.annotation.Generated
import org.greenrobot.greendao.annotation.Id
import org.greenrobot.greendao.annotation.Unique

/**
 * 书籍实体（greenDAO 表 PDF）。
 *
 * 改动前务必确认以下约束：
 * - **属性名即表列名与 JSON 字段名**：`common/greendao` 下已生成的 DAO 按列名读写，`Backup` 备份文件由 Gson 按字段名序列化，均不得改名
 * - **属性保持可变且有默认值**：保证无参构造可用，且 greenDAO 生成的 DAO 能继续通过 getter/setter 读写
 * - **字符串一律可空**：历史数据的 `cover` / `bookmark` / `dir` 等列可能为 NULL，DAO 会把 null 原样回填
 * - **Parcelable 读写顺序与历史版本一致**：否则升级后旧状态（`onSaveInstanceState`）恢复会错位
 *
 * @author sigpher
 */
@Entity
class PDF() : Parcelable {

    @field:Id(autoincrement = true)
    var id: Long? = null

    @field:Unique
    var path: String? = null
    var dir: String? = null
    var name: String? = null
    var cover: String? = null
    var progress: String? = null
    var curPage: Int = 0
    var totalPage: Int = 0
    var bookmark: String? = null
    var latestRead: Long = 0
    var position: Int = 0
    var scaleFactor: Float = 0f

    /**
     * greenDAO 生成的 DAO 用该全参构造读取一条记录（字段顺序与表列顺序一致）
     */
    @Generated(hash = 68430817)
    constructor(
        id: Long?,
        path: String?,
        dir: String?,
        name: String?,
        cover: String?,
        progress: String?,
        curPage: Int,
        totalPage: Int,
        bookmark: String?,
        latestRead: Long,
        position: Int,
        scaleFactor: Float
    ) : this() {
        this.id = id
        this.path = path
        this.dir = dir
        this.name = name
        this.cover = cover
        this.progress = progress
        this.curPage = curPage
        this.totalPage = totalPage
        this.bookmark = bookmark
        this.latestRead = latestRead
        this.position = position
        this.scaleFactor = scaleFactor
    }

    protected constructor(parcel: Parcel) : this() {
        this.id = parcel.readValue(Long::class.java.classLoader) as Long?
        this.path = parcel.readString()
        this.dir = parcel.readString()
        this.name = parcel.readString()
        this.cover = parcel.readString()
        this.progress = parcel.readString()
        this.curPage = parcel.readInt()
        this.totalPage = parcel.readInt()
        this.bookmark = parcel.readString()
        this.latestRead = parcel.readLong()
        this.position = parcel.readInt()
        this.scaleFactor = parcel.readFloat()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeValue(this.id)
        dest.writeString(this.path)
        dest.writeString(this.dir)
        dest.writeString(this.name)
        dest.writeString(this.cover)
        dest.writeString(this.progress)
        dest.writeInt(this.curPage)
        dest.writeInt(this.totalPage)
        dest.writeString(this.bookmark)
        dest.writeLong(this.latestRead)
        dest.writeInt(this.position)
        dest.writeFloat(this.scaleFactor)
    }

    override fun toString(): String {
        return "PDF{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", dir='" + dir + '\'' +
                ", name='" + name + '\'' +
                ", cover='" + cover + '\'' +
                ", progress='" + progress + '\'' +
                ", curPage=" + curPage +
                ", totalPage=" + totalPage +
                ", bookmark='" + bookmark + '\'' +
                ", latestRead=" + latestRead +
                ", position=" + position +
                ", scaleFactor=" + scaleFactor +
                '}'
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<PDF> = object : Parcelable.Creator<PDF> {
            override fun createFromParcel(source: Parcel): PDF = PDF(source)

            override fun newArray(size: Int): Array<PDF?> = arrayOfNulls(size)
        }
    }
}
