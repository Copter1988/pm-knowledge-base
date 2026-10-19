把中文字体放这里（只放一个即可，文件名保持 simhei.ttf）

为什么要放：
  导出 PDF 用 Flying Saucer 把 HTML 渲染成 PDF，PDF 必须「内嵌字体」才能显示中文。
  服务器/实验室环境通常没有中文字体，不放的话 PDF 里中文会变成方框或空白。

怎么拿：
  Windows 本机直接复制即可（黑体是单个 .ttf，Flying Saucer 能用）：
      copy C:\Windows\Fonts\simhei.ttf  E:\天平分析系统\src\main\resources\fonts\simhei.ttf

  注意 C:\Windows\Fonts\simsun.ttc 是「字体集合」（.ttc），Flying Saucer 不支持，别用。

字体解析顺序（PdfExportService 自动按此顺序找）：
  1) application.yml 的 app.pdf-font-path（默认 /fonts/simhei.ttf，即本目录）
  2) C:/Windows/Fonts/simhei.ttf
  3) C:/Windows/Fonts/msyh.ttc 无（不支持）
  都找不到时：PDF 仍会生成，但中文可能显示为方框，控制台会打印一行警告。

想换成开源字体（可随项目分发、无版权风险）：
  下载 Noto Sans SC / 思源黑体 的 .ttf，重命名或改 app.pdf-font-path 指向它即可。
