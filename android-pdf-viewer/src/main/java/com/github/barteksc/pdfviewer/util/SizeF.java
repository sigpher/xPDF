/**
 * Copyright 2017 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer.util;

/**
 * 浮点尺寸。原先来自 pdfium 绑定库的 com.shockwave.pdfium.util.SizeF，
 * 迁移到 io.legere:pdfiumandroid 后该包不再提供，故在本模块内自带一份等价实现。
 */
public class SizeF {

    private final float width;
    private final float height;

    public SizeF(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SizeF sizeF = (SizeF) o;
        return Float.compare(sizeF.width, width) == 0 && Float.compare(sizeF.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Float.floatToIntBits(width) + Float.floatToIntBits(height);
    }

    @Override
    public String toString() {
        return "SizeF{" + "width=" + width + ", height=" + height + '}';
    }
}
