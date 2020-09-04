# LongScreenshotTemp
记录中转：
Bitmap b = Bitmap.createBitmap(temp, 0, temp.getHeight() - scrollDistance, temp.getWidth(), scrollDistance);//裁剪出滚出来的距离内容。
前面通过这样裁剪图片
最后一张则反过来 截全屏，总长度-（temp.getHeight() - scrollDistance）
