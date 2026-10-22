@echo off
cd /d f:\Projects\xPDF
del build_check.bat build_check.log metrics.ps1 >nul 2>&1
git add -A
git commit -q -m "refactor(preview): 抽出书签角标渲染与进度持久化，PreviewActivity 去掉直连 DB" -m "- 新增 preview/BookmarkBadgeRenderer：位图解码一次、绘制只算坐标，recycle 由它自己负责" -m "- 新增 data/PreviewRepository：进度百分比/curPage/bookmark/scaleFactor 的写库与缓存刷新收敛到一处，后续换 suspend+IO 只改这里" -m "- PreviewActivity 删除 bookmarkBitmap/paint/getPercent 与 BitmapFactory/NumberFormat/Paint import" -m "- 度量：dex 类数 2780 -> 2781，APK 5.120MB -> 5.121MB"
git log --oneline -1
