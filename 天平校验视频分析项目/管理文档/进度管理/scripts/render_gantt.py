from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path

import matplotlib.dates as mdates
import matplotlib.pyplot as plt
from matplotlib.patches import Patch


@dataclass(frozen=True)
class Task:
    name: str
    plan_start: date
    plan_end: date
    actual_start: date
    actual_end: date
    status: str


TASKS = [
    Task("需求澄清与 SRS", date(2026, 9, 28), date(2026, 10, 12),
         date(2026, 9, 28), date(2026, 10, 14), "done"),
    Task("系统设计", date(2026, 10, 12), date(2026, 10, 16),
         date(2026, 10, 12), date(2026, 10, 19), "done"),
    Task("甲模块开发", date(2026, 10, 15), date(2026, 11, 11),
         date(2026, 10, 12), date(2026, 10, 22), "done"),
    Task("乙模块与前端", date(2026, 10, 16), date(2026, 11, 11),
         date(2026, 10, 23), date(2026, 10, 26), "done"),
    Task("单元/集成测试", date(2026, 11, 11), date(2026, 11, 18),
         date(2026, 10, 24), date(2026, 10, 26), "done"),
    Task("三视频系统测试", date(2026, 11, 18), date(2026, 11, 20),
         date(2026, 10, 24), date(2026, 10, 24), "done"),
    Task("最终验收 UAT", date(2026, 11, 20), date(2026, 11, 24),
         date(2026, 11, 3), date(2026, 11, 6), "forecast"),
    Task("部署与回滚演练", date(2026, 11, 24), date(2026, 11, 26),
         date(2026, 11, 9), date(2026, 11, 10), "forecast"),
    Task("文档与汇报视频", date(2026, 11, 26), date(2026, 11, 27),
         date(2026, 11, 11), date(2026, 11, 12), "forecast"),
    Task("冻结与交付", date(2026, 11, 27), date(2026, 11, 29),
         date(2026, 11, 13), date(2026, 11, 16), "forecast"),
]

PLAN_COLOR = "#3B82F6"
DONE_COLOR = "#10B981"
FORECAST_COLOR = "#F59E0B"
GRID_COLOR = "#E5E7EB"
TEXT_COLOR = "#1F2937"
MUTED_COLOR = "#64748B"
TODAY = date(2026, 11, 4)


def add_weekend_shading(ax: plt.Axes, start: date, end: date) -> None:
    current = start
    while current <= end:
        if current.weekday() == 5:
            ax.axvspan(current, current + timedelta(days=2),
                       color="#F8FAFC", zorder=0)
        current += timedelta(days=1)


def add_bar(ax: plt.Axes, y: float, start: date, end: date,
            color: str, height: float, alpha: float = 1.0,
            hatch: str | None = None) -> None:
    duration = max((end - start).days + 1, 0.7)
    ax.barh(y, duration, left=start, height=height, color=color,
            alpha=alpha, hatch=hatch, edgecolor="white", linewidth=0.8,
            align="center", zorder=3)


def finish_delta(task: Task) -> str:
    delta = (task.plan_end - task.actual_end).days
    if delta > 0:
        return f"提前 {delta} 天"
    if delta < 0:
        return f"延后 {abs(delta)} 天"
    return "按计划"


def main() -> None:
    plt.rcParams["font.sans-serif"] = ["SimHei", "Microsoft YaHei", "Arial Unicode MS"]
    plt.rcParams["axes.unicode_minus"] = False

    fig, ax = plt.subplots(figsize=(16, 9.5), dpi=220)
    fig.patch.set_facecolor("white")
    ax.set_facecolor("white")

    chart_start = date(2026, 9, 28)
    chart_end = date(2026, 12, 20)
    add_weekend_shading(ax, chart_start, chart_end)

    row_positions = list(range(len(TASKS) - 1, -1, -1))
    for y, task in zip(row_positions, TASKS):
        add_bar(ax, y + 0.19, task.plan_start, task.plan_end,
                PLAN_COLOR, 0.27, alpha=0.92)
        if task.status == "done":
            add_bar(ax, y - 0.19, task.actual_start, task.actual_end,
                    DONE_COLOR, 0.27, alpha=0.95)
        else:
            add_bar(ax, y - 0.19, task.actual_start, task.actual_end,
                    FORECAST_COLOR, 0.27, alpha=0.65, hatch="///")

        actual_label = "实际" if task.status == "done" else "预测"
        ax.text(task.actual_end + timedelta(days=1.2), y - 0.19,
                f"{actual_label} {finish_delta(task)}",
                va="center", ha="left", fontsize=8.2, color=MUTED_COLOR)

    ax.set_yticks(row_positions)
    ax.set_yticklabels([task.name for task in TASKS], fontsize=10.5, color=TEXT_COLOR)
    ax.set_ylim(-0.75, len(TASKS) - 0.25)
    ax.set_xlim(chart_start, chart_end)

    ax.axvline(TODAY, color="#DC2626", linewidth=1.8, linestyle="--", zorder=5)
    ax.text(TODAY, len(TASKS) - 0.38, " 数据截止 11-04",
            color="#B91C1C", fontsize=9.2, va="bottom", ha="left",
            bbox=dict(facecolor="white", edgecolor="none", alpha=0.9, pad=1.4))

    milestones = [
        (date(2026, 10, 14), "M1"),
        (date(2026, 10, 19), "M2"),
        (date(2026, 10, 26), "M3"),
        (date(2026, 11, 6), "M4"),
        (date(2026, 11, 10), "M5"),
        (date(2026, 11, 16), "M6"),
        (date(2026, 12, 9), "M7"),
    ]
    for milestone_date, label in milestones:
        ax.scatter(milestone_date, len(TASKS) - 0.43, marker="D", s=42,
                   color="#111827", zorder=6, clip_on=False)
        ax.text(milestone_date, len(TASKS) - 0.18, label,
                ha="center", va="bottom", fontsize=8, color="#111827")

    ax.xaxis.set_major_locator(mdates.WeekdayLocator(byweekday=mdates.MO))
    ax.xaxis.set_major_formatter(mdates.DateFormatter("%m-%d"))
    ax.xaxis.set_minor_locator(mdates.DayLocator(interval=1))
    ax.tick_params(axis="x", labelsize=9, colors=MUTED_COLOR)
    ax.tick_params(axis="y", length=0)
    ax.grid(axis="x", which="major", color=GRID_COLOR, linewidth=1.0, zorder=1)
    ax.grid(axis="y", color="#F1F5F9", linewidth=0.8, zorder=0)
    for spine in ("top", "right", "left"):
        ax.spines[spine].set_visible(False)
    ax.spines["bottom"].set_color("#CBD5E1")

    legend = [
        Patch(facecolor=PLAN_COLOR, label="基准计划"),
        Patch(facecolor=DONE_COLOR, label="实际完成"),
        Patch(facecolor=FORECAST_COLOR, alpha=0.65, hatch="///",
              label="当前/预测"),
    ]
    ax.legend(handles=legend, loc="upper right", frameon=False,
              ncol=3, fontsize=10, bbox_to_anchor=(1.0, 1.055))

    fig.suptitle("天平校验视频分析系统 · 计划与实际甘特图",
                 x=0.06, y=0.985, ha="left", fontsize=20, fontweight="bold",
                 color=TEXT_COLOR)
    fig.text(0.06, 0.932,
             "截至 2026-11-04｜编码与三视频系统测试已完成｜78 Java tests + 5 OCR tests 全部通过",
             fontsize=10.5, color=MUTED_COLOR, ha="left")
    fig.text(0.06, 0.018,
             "说明：计划条与对应实际/预测条上下排布；橙色斜纹表示尚未完成，日期以工作日计划为基准展示。",
             fontsize=8.5, color=MUTED_COLOR, ha="left")

    fig.subplots_adjust(left=0.19, right=0.89, top=0.855, bottom=0.08)

    output = Path(__file__).resolve().parents[1] / "项目进度甘特图_计划与实际_2026-11-04.png"
    fig.savefig(output, dpi=220, facecolor="white", bbox_inches="tight")
    plt.close(fig)
    print(output)


if __name__ == "__main__":
    main()
