$root = 'f:\Projects\xPDF\app\src\main\java'
$targets = @('DataManager','UiManager','PdfUtils','DialogUtils','App\b','AppConfig','GroupingAdapter','EmptyHolder','HeaderHolder','XGridDecoration','YGridDecoration','Bookmark','BorderImageView','ImageTextView','NewViewPager','PDF','Collection','RecentPDF','Cover')
foreach ($t in $targets) {
    $hits = (Get-ChildItem -Path $root -Recurse -Include *.kt,*.java |
        Select-String -Pattern $t -Encoding UTF8)
    $files = ($hits | Select-Object -ExpandProperty Path -Unique).Count
    Write-Output ("{0,-18} refs={1,-4} files={2}" -f $t, $hits.Count, $files)
}
