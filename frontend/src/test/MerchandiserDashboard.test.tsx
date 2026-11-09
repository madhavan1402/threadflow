import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react';
import { MerchandiserDashboard } from '../components/MerchandiserDashboard';
import { analyticsApi, type ProductionSummary, type OrderDelayRisk } from '../api/analytics';
import { productionApi, type ProductionUpdate } from '../api/production';
import { ordersApi, type OrderPage } from '../api/orders';

vi.mock('../api/analytics', () => ({
  analyticsApi: {
    summary: vi.fn(),
    allOrderRisks: vi.fn(),
  },
}));

vi.mock('../api/production', () => ({
  productionApi: {
    updates: vi.fn(),
  },
}));

vi.mock('../api/orders', () => ({
  ordersApi: {
    list: vi.fn(),
  },
}));

describe('MerchandiserDashboard Component', () => {
  const mockSummary: ProductionSummary = {
    calculatedForDate: '2026-03-20',
    totalOrders: 10,
    onTrack: 6,
    atRisk: 3,
    delayed: 1,
    completed: 2,
  };

  const mockRisks: OrderDelayRisk[] = [
    {
      orderId: 101,
      poNumber: 'PO-88240',
      buyerName: 'Company B',
      styleName: 'Cotton Polo',
      exFactoryDate: '2026-04-10',
      calculatedForDate: '2026-03-20',
      finalMilestoneName: 'Packing',
      totalQuantity: 10000,
      reportedCompletedQuantity: 4000,
      completedQuantity: 4000,
      remainingQuantity: 6000,
      elapsedProductionDays: 10,
      remainingProductionDays: 15,
      requiredDailyRate: 400,
      actualDailyRate: 350,
      estimatedCompletionDays: 17,
      estimatedCompletionDate: '2026-04-12',
      productionShortfall: 500,
      completionPercent: 40,
      riskStatus: 'AT_RISK',
      reasons: ['Knitting line 2 running below required speed'],
    },
  ];

  const mockUpdates: ProductionUpdate[] = [
    {
      id: 501,
      orderId: 101,
      poNumber: 'PO-88240',
      milestoneId: 2,
      milestoneName: 'Knitting',
      supervisorId: 3,
      supervisorEmail: 'supervisor@threadflow.com',
      productionDate: '2026-03-20',
      completedQuantity: 850,
      issueNote: 'நூல் தரம் குறைவாக உள்ளது (Yarn quality issue)',
      createdAt: '2026-03-20T10:00:00Z',
      updatedAt: '2026-03-20T10:00:00Z',
      photos: [],
    },
  ];

  const mockOrdersPage: OrderPage = {
    content: [
      {
        id: 101,
        poNumber: 'PO-88240',
        buyer: { id: 2, buyerName: 'Company B', country: 'UK' },
        styleName: 'Cotton Polo',
        totalQuantity: 10000,
        exFactoryDate: '2026-04-10',
        status: 'IN_PROGRESS',
        variants: [],
        milestones: [
          {
            id: 1,
            milestoneName: 'Yarn',
            sequenceNumber: 1,
            plannedStartDate: '2026-03-01',
            plannedEndDate: '2026-03-10',
            status: 'COMPLETED',
          },
          {
            id: 2,
            milestoneName: 'Knitting',
            sequenceNumber: 2,
            plannedStartDate: '2026-03-11',
            plannedEndDate: '2026-03-25',
            status: 'IN_PROGRESS',
          },
        ],
      },
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders loading state initially', () => {
    vi.mocked(analyticsApi.summary).mockReturnValue(new Promise(() => {}));
    vi.mocked(analyticsApi.allOrderRisks).mockReturnValue(new Promise(() => {}));
    vi.mocked(productionApi.updates).mockReturnValue(new Promise(() => {}));
    vi.mocked(ordersApi.list).mockReturnValue(new Promise(() => {}));

    render(
      <MerchandiserDashboard
        onOpenOrder={vi.fn()}
        onCreateOrder={vi.fn()}
      />
    );

    expect(screen.getByRole('status')).toBeDefined();
    expect(screen.getByText(/Assembling production intelligence/)).toBeDefined();
  });

  it('loads and displays metrics, charts, watchlist and live floor feed', async () => {
    vi.mocked(analyticsApi.summary).mockResolvedValue(mockSummary);
    vi.mocked(analyticsApi.allOrderRisks).mockResolvedValue(mockRisks);
    vi.mocked(productionApi.updates).mockResolvedValue(mockUpdates);
    vi.mocked(ordersApi.list).mockResolvedValue(mockOrdersPage);

    const onOpenOrder = vi.fn();
    const onCreateOrder = vi.fn();

    render(
      <MerchandiserDashboard
        onOpenOrder={onOpenOrder}
        onCreateOrder={onCreateOrder}
        pendingIssuesCount={2}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 2, name: /Production Intelligence/ })).toBeDefined();
      expect(screen.getByText('Active Orders')).toBeDefined();
      expect(screen.getAllByText('On Track').length).toBeGreaterThan(0);
      expect(screen.getAllByText('At Risk').length).toBeGreaterThan(0);
      expect(screen.getByText('Delayed Orders')).toBeDefined();
    });

    // Check review issues button
    expect(screen.getByTestId('dash-review-issues-btn')).toBeDefined();
    expect(screen.getByText(/Review Tamil Issues \(2\)/)).toBeDefined();

    // Check create order button
    const createBtn = screen.getByTestId('dash-create-order-btn');
    fireEvent.click(createBtn);
    expect(onCreateOrder).toHaveBeenCalledTimes(1);

    // Check watchlist row & data rows
    expect(screen.getAllByText('PO-88240').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Company B').length).toBeGreaterThan(0);

    // Click Details button
    const detailsBtn = screen.getByRole('button', { name: /Details/ });
    fireEvent.click(detailsBtn);
    expect(onOpenOrder).toHaveBeenCalledWith(101);

    // Check shop floor live feed
    expect(screen.getByText('+850 pcs')).toBeDefined();
    expect(screen.getByText(/நூல் தரம் குறைவாக உள்ளது/)).toBeDefined();
  });

  it('renders error state and supports retry', async () => {
    vi.mocked(analyticsApi.summary).mockRejectedValueOnce(new Error('Backend offline'));
    vi.mocked(analyticsApi.allOrderRisks).mockResolvedValue([]);
    vi.mocked(productionApi.updates).mockResolvedValue([]);
    vi.mocked(ordersApi.list).mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0 });

    render(
      <MerchandiserDashboard
        onOpenOrder={vi.fn()}
        onCreateOrder={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeDefined();
      expect(screen.getByText('Dashboard Analytics Unavailable')).toBeDefined();
    });

    vi.mocked(analyticsApi.summary).mockResolvedValue(mockSummary);
    const retryBtn = screen.getByRole('button', { name: 'Retry Connection' });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 2, name: /Production Intelligence/ })).toBeDefined();
    });
  });
});
