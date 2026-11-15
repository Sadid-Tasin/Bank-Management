
"use client";

import { useEffect, useId, useMemo, useRef, useState } from "react";
import type { KeyboardEvent, PointerEvent } from "react";

export type ActivityPoint = { date: string; deposits: number; withdrawals: number };
type Range = 7 | 14 | 30;

const H = 270;
const PL = 52;
const PR = 16;
const PT = 18;
const PB = 34;
const GREEN = "#1b9a70";
const LAV = "#8f9fe6";

const bdt = (v: number) =>
  new Intl.NumberFormat("en-BD", { style: "currency", currency: "BDT", maximumFractionDigits: 2 }).format(v);
const compact = (v: number) =>
  new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 }).format(v);
const parse = (s: string) => new Date(`${s}T00:00:00`);
const dayShort = (s: string) => parse(s).toLocaleDateString("en-GB", { day: "2-digit", month: "short" });
const dayLong = (s: string) =>
  parse(s).toLocaleDateString("en-GB", { weekday: "long", day: "2-digit", month: "long", year: "numeric" });

function niceMax(v: number) {
  if (v <= 0) return 4;
  const p = Math.pow(10, Math.floor(Math.log10(v)));
  const n = v / p;
  return (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * p;
}

export default function ActivityChart({
  data,
  fallbackDeposits = 0,
  fallbackWithdrawals = 0,
}: {
  data: ActivityPoint[];
  fallbackDeposits?: number;
  fallbackWithdrawals?: number;
}) {
  const uid = useId().replace(/:/g, "");
  const [range, setRange] = useState<Range>(30);
  const [width, setWidth] = useState(640);
  const [active, setActive] = useState<number | null>(null);
  const [hidden, setHidden] = useState({ d: false, w: false });
  const box = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = box.current;
    if (!el) return;
    const ro = new ResizeObserver(([entry]) => setWidth(Math.max(280, Math.round(entry.contentRect.width))));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const points = useMemo(
    () => data.map((p) => ({ date: p.date, deposits: Number(p.deposits) || 0, withdrawals: Number(p.withdrawals) || 0 })).slice(-range),
    [data, range],
  );
  const n = points.length;
  const hasData = points.some((p) => p.deposits > 0 || p.withdrawals > 0);

  const visibleValues = points.flatMap((p) => [hidden.d ? 0 : p.deposits, hidden.w ? 0 : p.withdrawals]);
  const max = niceMax(Math.max(0, ...visibleValues));
  const innerW = width - PL - PR;
  const innerH = H - PT - PB;
  const x = (i: number) => (n <= 1 ? PL + innerW / 2 : PL + (i * innerW) / (n - 1));
  const y = (v: number) => PT + innerH - (v / max) * innerH;

  const line = (key: "deposits" | "withdrawals") =>
    points
      .map((p, i) => {
        const px = x(i);
        const py = y(p[key]);
        if (i === 0) return `M${px},${py}`;
        const qx = x(i - 1);
        const qy = y(points[i - 1][key]);
        const cx = (qx + px) / 2;
        return `C${cx},${qy} ${cx},${py} ${px},${py}`;
      })
      .join(" ");
  const area = (key: "deposits" | "withdrawals") =>
    n > 1 ? `${line(key)} L${x(n - 1)},${PT + innerH} L${x(0)},${PT + innerH} Z` : "";

  const totalD = n ? points.reduce((s, p) => s + p.deposits, 0) : range === 30 ? fallbackDeposits : 0;
  const totalW = n ? points.reduce((s, p) => s + p.withdrawals, 0) : range === 30 ? fallbackWithdrawals : 0;

  const onMove = (e: PointerEvent<SVGSVGElement>) => {
    if (!n) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const i = Math.round(((e.clientX - rect.left - PL) / innerW) * (n - 1));
    setActive(Math.min(n - 1, Math.max(0, Number.isFinite(i) ? i : 0)));
  };
  const onKey = (e: KeyboardEvent<SVGSVGElement>) => {
    if (!n) return;
    if (e.key === "ArrowLeft") { e.preventDefault(); setActive((a) => Math.max(0, (a ?? n) - 1)); }
    if (e.key === "ArrowRight") { e.preventDefault(); setActive((a) => Math.min(n - 1, (a ?? -1) + 1)); }
    if (e.key === "Escape") setActive(null);
  };

  const step = Math.max(1, Math.ceil(n / 6));
  const a = active !== null ? points[active] : null;
  const ax = active !== null ? x(active) : 0;
  const tipW = width < 420 ? 168 : 196;
  const tipLeft = ax < width / 2 ? ax + 18 : ax - 18 - tipW;
  const bandW = n > 1 ? innerW / (n - 1) : 48;
  const chipLabel = a ? dayShort(a.date) : "";

  return (
    <div className="ac">
      <div className="ac-top">
        <div>
          <strong className="ac-total">{bdt(totalD + totalW)}</strong>
          <small>combined transaction volume · last {range} days</small>
        </div>
        <div className="ac-seg" role="tablist" aria-label="Chart range">
          {([7, 14, 30] as Range[]).map((r) => (
            <button key={r} role="tab" aria-selected={range === r} className={range === r ? "on" : ""} onClick={() => { setRange(r); setActive(null); }}>
              {r}d
            </button>
          ))}
        </div>
      </div>

      <div className="ac-box" ref={box} style={{ height: H }}>
        <svg
          className="ac-svg"
          width={width}
          height={H}
          viewBox={`0 0 ${width} ${H}`}
          role="img"
          aria-label="Deposits and withdrawals per day. Use left and right arrow keys to inspect each day."
          tabIndex={0}
          onPointerMove={onMove}
          onPointerDown={onMove}
          onPointerLeave={() => setActive(null)}
          onKeyDown={onKey}
          onBlur={() => setActive(null)}
        >
          <defs>
            <linearGradient id={`${uid}-g`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={GREEN} stopOpacity=".32" />
              <stop offset="1" stopColor={GREEN} stopOpacity="0" />
            </linearGradient>
            <linearGradient id={`${uid}-l`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={LAV} stopOpacity=".26" />
              <stop offset="1" stopColor={LAV} stopOpacity="0" />
            </linearGradient>
          </defs>

          {[0, 1, 2, 3, 4].map((k) => {
            const yy = PT + innerH - (k * innerH) / 4;
            return (
              <g key={k}>
                <line className="ac-grid" x1={PL} x2={width - PR} y1={yy} y2={yy} />
                <text className="ac-axis" x={PL - 10} y={yy + 3.5} textAnchor="end">{k === 0 ? "0" : compact((max * k) / 4)}</text>
              </g>
            );
          })}

          {points.map((p, i) =>
            i % step === (n - 1) % step ? (
              <text key={p.date} className="ac-axis" x={x(i)} y={H - 12} textAnchor="middle">{dayShort(p.date)}</text>
            ) : null,
          )}

          <g key={`${range}-${hidden.d}-${hidden.w}`}>
            {!hidden.w && n > 1 && <path className="ac-area" d={area("withdrawals")} fill={`url(#${uid}-l)`} />}
            {!hidden.d && n > 1 && <path className="ac-area" d={area("deposits")} fill={`url(#${uid}-g)`} />}
            {!hidden.w && <path className="ac-line" pathLength={1} d={line("withdrawals")} stroke={LAV} />}
            {!hidden.d && <path className="ac-line" pathLength={1} d={line("deposits")} stroke={GREEN} style={{ animationDelay: ".08s" }} />}
          </g>

          {a && (
            <g pointerEvents="none">
              <rect className="ac-band" x={ax - bandW / 2} y={PT} width={bandW} height={innerH} rx={8} />
              <line className="ac-guide" x1={ax} x2={ax} y1={PT} y2={PT + innerH} />
              {!hidden.w && (
                <g key={`w${active}`}>
                  <circle className="ac-halo" cx={ax} cy={y(a.withdrawals)} r={13} fill={LAV} />
                  <circle className="ac-dot" cx={ax} cy={y(a.withdrawals)} r={6.5} fill="#fff" stroke={LAV} strokeWidth={3.5} />
                </g>
              )}
              {!hidden.d && (
                <g key={`d${active}`}>
                  <circle className="ac-halo" cx={ax} cy={y(a.deposits)} r={13} fill={GREEN} />
                  <circle className="ac-dot" cx={ax} cy={y(a.deposits)} r={6.5} fill="#fff" stroke={GREEN} strokeWidth={3.5} />
                </g>
              )}
              <rect x={ax - 28} y={H - 25} width={56} height={20} rx={10} fill="#16866d" />
              <text x={ax} y={H - 11} textAnchor="middle" fill="#fff" fontSize={10} fontWeight={700}>{chipLabel}</text>
            </g>
          )}
        </svg>

        {a && (
          <div className="ac-tip" style={{ left: tipLeft, width: tipW, transformOrigin: ax < width / 2 ? "left top" : "right top" }} role="status">
            <div className="ac-tip-date">{dayLong(a.date)}</div>
            <div className="ac-tip-row"><span><i style={{ background: GREEN }} />Deposits</span><b>{bdt(a.deposits)}</b></div>
            <div className="ac-tip-row"><span><i style={{ background: LAV }} />Withdrawals</span><b>{bdt(a.withdrawals)}</b></div>
            <div className="ac-tip-row ac-tip-net"><span>Net flow</span><b className={a.deposits - a.withdrawals >= 0 ? "pos" : "neg"}>{a.deposits - a.withdrawals >= 0 ? "+" : "−"}{bdt(Math.abs(a.deposits - a.withdrawals))}</b></div>
          </div>
        )}

        {(!hasData || n === 0) && (
          <div className="ac-empty">
            <span>{n === 0 ? "Daily breakdown is not available yet. Showing 30-day totals only." : "No activity yet. New deposits and withdrawals will appear here."}</span>
          </div>
        )}
      </div>

      <div className="ac-legend">
        <button type="button" className={hidden.d ? "off" : ""} onClick={() => setHidden((h) => ({ ...h, d: !h.d }))}>
          <i style={{ background: GREEN }} />Deposits <b>{bdt(totalD)}</b>
        </button>
        <button type="button" className={hidden.w ? "off" : ""} onClick={() => setHidden((h) => ({ ...h, w: !h.w }))}>
          <i style={{ background: LAV }} />Withdrawals <b>{bdt(totalW)}</b>
        </button>
      </div>
    </div>
  );
}
