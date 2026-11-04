export type AnalyticsDateRange = '30' | '90' | 'all';

export function getAnalyticsDateRange(
  range: AnalyticsDateRange,
  today = new Date(),
): { from?: string; to?: string } {
  if (range === 'all') {
    return {};
  }

  const to = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
  const from = new Date(to);
  from.setUTCDate(from.getUTCDate() - Number(range) + 1);

  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10),
  };
}
