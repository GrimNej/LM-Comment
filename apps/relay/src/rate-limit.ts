export class DailyRequestCounter {
  private date = '';
  private count = 0;

  constructor(private readonly now: () => Date = () => new Date()) {}

  tryIncrement(maximum: number): { allowed: boolean; currentCount: number } {
    const currentDate = this.now().toISOString().slice(0, 10);
    if (currentDate !== this.date) {
      this.date = currentDate;
      this.count = 0;
    }

    if (this.count >= maximum) {
      return { allowed: false, currentCount: this.count };
    }

    this.count += 1;
    return { allowed: true, currentCount: this.count };
  }
}
