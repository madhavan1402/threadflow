import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react';
import { BuyerPortal } from '../components/BuyerPortal';
import { OrderAuditHistoryModal } from '../components/OrderAuditHistoryModal';
import { BuyerWarningPanel } from '../components/BuyerWarningPanel';
import { buyerApi, BuyerOverview, BuyerOrderSummary, BuyerOrderDetail } from '../api/buyer';
import { auditApi, DateAuditRecord } from '../api/audit';
import { warningsApi, BuyerDelayWarning } from '../api/warnings';
import { ProductionOrder } from '../api/orders';

vi.mock('../api/buyer', async () => {
  const actual = await vi.importActual<typeof import('../api/buyer')>('../api/buyer');
  return {
    ...actual,
    buyerApi: {
      getOverview: vi.fn(),
      getOrders: vi.fn(),
      getOrderDetail: vi.fn(),
      photoContentUrl: vi.fn((orderId, photoId, token) => `/api/v1/buyer/orders/${orderId}/photos/${photoId}/content?token=${token}`),
    },
  };
});

vi.mock('../api/audit', async () => {
  const actual = await vi.importActual<typeof import('../api/audit')>('../api/audit');
  return {
    ...actual,
    auditApi: {
      getAuditHistory: vi.fn(),
      changeExFactoryDate: vi.fn(),
    },
  };
});

vi.mock('../api/warnings', async () => {
  const actual = await vi.importActual<typeof import('../api/warnings')>('../api/warnings');
  return {
    ...actual,
    warningsApi: {
      getWarnings: vi.fn(),
      createDraft: vi.fn(),
      confirmWarning: vi.fn(),
      dismissWarning: vi.fn(),
    },
  };
});

describe('BuyerPortal Component', () => {
  const mockOverview: BuyerOverview = {
    buyerId: 2,
    buyerName: 'Company B',
    country: 'United Kingdom',
    totalOrders: 3,
    inProgressOrders: 2,
    completedOrders: 1,
    confirmedWarningsCount: 1,
  };

  const mockOrders: BuyerOrderSummary[] = [
    {
      id: 201,
      poNumber: 'B-PO-88240',
      styleName: '100% Cotton Ribbed Polo',
      productDescription: 'Export grade polo',
      totalQuantity: 12000,
      exFactoryDate: '2026-05-15',
      status: 'IN_PROGRESS',
      progressPercent: 45,
      confirmedWarningsCount: 1,
      approvedPhotosCount: 2,
    },
    {
      id: 202,
      poNumber: 'B-PO-88241',
      styleName: 'Organic Cotton Crewneck',
      productDescription: 'Basic crewneck',
      totalQuantity: 8000,
      exFactoryDate: '2026-06-01',
      status: 'PLANNED',
      progressPercent: 10,
      confirmedWarningsCount: 0,
      approvedPhotosCount: 0,
    },
  ];

  const mockDetail: BuyerOrderDetail = {
    id: 201,
    poNumber: 'B-PO-88240',
    styleName: '100% Cotton Ribbed Polo',
    productDescription: 'Export grade polo',
    totalQuantity: 12000,
    exFactoryDate: '2026-05-15',
    status: 'IN_PROGRESS',
    progressPercent: 45,
    milestones: [
      {
        id: 1,
        milestoneName: 'Yarn Sourcing & Testing',
        sequenceNumber: 1,
        plannedStartDate: '2026-03-01',
        plannedEndDate: '2026-03-10',
        actualCompletionDate: '2026-03-09',
        status: 'COMPLETED',
      },
      {
        id: 2,
        milestoneName: 'Knitting & Dyeing',
        sequenceNumber: 2,
        plannedStartDate: '2026-03-11',
        plannedEndDate: '2026-03-25',
        actualCompletionDate: null,
        status: 'IN_PROGRESS',
      },
    ],
    confirmedWarnings: [
      {
        id: 901,
        riskStatus: 'MEDIUM',
        message: 'Dyeing stage is tracking 3 days behind due to lab dip revision. Factory has allocated overtime knitting lines to protect final vessel booking.',
        confirmedAt: '2026-03-18T10:00:00Z',
      },
    ],
    approvedPhotos: [
      {
        id: 501,
        originalFilename: 'fabric_inspection_lot1.jpg',
        contentType: 'image/jpeg',
        fileSize: 204800,
        approvedAt: '2026-03-18T12:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders portal with header, preset chips and loads initial data', async () => {
    vi.mocked(buyerApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(buyerApi.getOrders).mockResolvedValue(mockOrders);

    render(<BuyerPortal />);

    expect(screen.getByText('Production Order Portal')).toBeDefined();
    expect(screen.getByTestId('demo-preset-buyer-token-company-b')).toBeDefined();
    expect(screen.getByTestId('demo-preset-buyer-token-company-c')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByTestId('portal-metric-strip')).toBeDefined();
      expect(screen.getByText('Company B')).toBeDefined();
      expect(screen.getByText('B-PO-88240')).toBeDefined();
      expect(screen.getByText('B-PO-88241')).toBeDefined();
    });

    expect(screen.getByText('12,000 pcs')).toBeDefined();
    expect(screen.getByText('1 alert')).toBeDefined();
  });

  it('filters orders by search term in list view', async () => {
    vi.mocked(buyerApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(buyerApi.getOrders).mockResolvedValue(mockOrders);

    render(<BuyerPortal />);

    await waitFor(() => {
      expect(screen.getByText('B-PO-88240')).toBeDefined();
      expect(screen.getByText('B-PO-88241')).toBeDefined();
    });

    const searchInput = screen.getByTestId('buyer-search-input');
    fireEvent.change(searchInput, { target: { value: 'Crewneck' } });

    expect(screen.queryByText('B-PO-88240')).toBeNull();
    expect(screen.getByText('B-PO-88241')).toBeDefined();
  });

  it('opens order detail view and shows confirmed delay advisory and approved photos', async () => {
    vi.mocked(buyerApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(buyerApi.getOrders).mockResolvedValue(mockOrders);
    vi.mocked(buyerApi.getOrderDetail).mockResolvedValue(mockDetail);

    render(<BuyerPortal />);

    await waitFor(() => {
      expect(screen.getByTestId('view-order-btn-201')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('view-order-btn-201'));

    await waitFor(() => {
      expect(screen.getByTestId('portal-order-detail')).toBeDefined();
      expect(screen.getByText('PO: B-PO-88240')).toBeDefined();
      expect(screen.getByTestId('portal-confirmed-warning-box')).toBeDefined();
      expect(screen.getByText(/Dyeing stage is tracking 3 days behind/)).toBeDefined();
      expect(screen.getByTestId('portal-milestones-table')).toBeDefined();
      expect(screen.getByText('Yarn Sourcing & Testing')).toBeDefined();
      expect(screen.getByText('Knitting & Dyeing')).toBeDefined();
      expect(screen.getByTestId('photo-card-501')).toBeDefined();
      expect(screen.getByText('fabric_inspection_lot1.jpg')).toBeDefined();
    });

    // Click photo card to open preview modal
    fireEvent.click(screen.getByTestId('photo-card-501'));
    expect(screen.getByRole('heading', { level: 4, name: 'fabric_inspection_lot1.jpg' })).toBeDefined();

    // Click back button to return to order list
    const backBtn = screen.getByTestId('portal-back-to-orders-btn');
    fireEvent.click(backBtn);

    await waitFor(() => {
      expect(screen.queryByTestId('portal-order-detail')).toBeNull();
      expect(screen.getByTestId('buyer-orders-table')).toBeDefined();
    });
  });

  it('displays error alert when token is invalid and supports retry', async () => {
    vi.mocked(buyerApi.getOverview).mockRejectedValueOnce(new Error('Invalid or revoked buyer access token'));
    vi.mocked(buyerApi.getOrders).mockResolvedValue([]);

    render(<BuyerPortal />);

    await waitFor(() => {
      expect(screen.getByTestId('portal-error-alert')).toBeDefined();
      expect(screen.getByText(/Invalid or revoked buyer access token/)).toBeDefined();
    });

    vi.mocked(buyerApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(buyerApi.getOrders).mockResolvedValue(mockOrders);

    const retryBtn = screen.getByTestId('portal-retry-btn');
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.queryByTestId('portal-error-alert')).toBeNull();
      expect(screen.getByTestId('portal-metric-strip')).toBeDefined();
    });
  });

  it('disconnects token and clears view when Disconnect button is clicked', async () => {
    vi.mocked(buyerApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(buyerApi.getOrders).mockResolvedValue(mockOrders);

    render(<BuyerPortal />);

    await waitFor(() => {
      expect(screen.getByTestId('portal-metric-strip')).toBeDefined();
    });

    const logoutBtn = screen.getByTestId('buyer-logout-btn');
    fireEvent.click(logoutBtn);

    expect(screen.queryByTestId('portal-metric-strip')).toBeNull();
    expect(screen.queryByTestId('buyer-orders-table')).toBeNull();
  });
});

describe('OrderAuditHistoryModal Component', () => {
  const mockOrder: ProductionOrder = {
    id: 101,
    poNumber: 'PO-99120',
    buyer: { id: 1, buyerName: 'Company A', country: 'US' },
    styleName: 'Cotton T-Shirt',
    totalQuantity: 15000,
    exFactoryDate: '2026-05-10',
    status: 'IN_PROGRESS',
    variants: [],
    milestones: [],
  };

  const mockAuditRecords: DateAuditRecord[] = [
    {
      id: 1,
      orderId: 101,
      previousExFactoryDate: '2026-05-01',
      newExFactoryDate: '2026-05-10',
      reason: 'Lab dip approval delayed by buyer testing lab by 9 days',
      changedByEmail: 'merchandiser@threadflow.com',
      changedAt: '2026-03-10T14:30:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders modal with date revision history', async () => {
    vi.mocked(auditApi.getAuditHistory).mockResolvedValue(mockAuditRecords);

    render(
      <OrderAuditHistoryModal
        order={mockOrder}
        onClose={vi.fn()}
        onDateUpdated={vi.fn()}
      />
    );

    expect(screen.getByTestId('audit-history-modal')).toBeDefined();
    expect(screen.getByText('Ex-Factory Date Audit History')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByTestId('audit-timeline-list')).toBeDefined();
      const record = screen.getByTestId('audit-record-1');
      expect(record.textContent).toContain('Lab dip approval delayed');
      expect(record.textContent).toContain('2026-05-01');
      expect(record.textContent).toContain('2026-05-10');
    });
  });

  it('submits a new ex-factory date revision with mandatory reason', async () => {
    vi.mocked(auditApi.getAuditHistory).mockResolvedValue([]);
    vi.mocked(auditApi.changeExFactoryDate).mockResolvedValue({
      id: 101,
      poNumber: 'PO-99120',
      previousExFactoryDate: '2026-05-10',
      newExFactoryDate: '2026-05-20',
      reason: 'Yarn spin mill delivery delayed',
      changedByEmail: 'merch@threadflow.com',
      changedAt: '2026-03-19T00:00:00Z',
    });

    const onDateUpdated = vi.fn();
    render(
      <OrderAuditHistoryModal
        order={mockOrder}
        onClose={vi.fn()}
        onDateUpdated={onDateUpdated}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('No ex-factory date changes have been recorded for this order yet.')).toBeDefined();
    });

    const dateInput = screen.getByTestId('audit-new-date-input');
    const reasonInput = screen.getByTestId('audit-reason-input');
    const submitBtn = screen.getByTestId('submit-audit-date-change');

    fireEvent.change(dateInput, { target: { value: '2026-05-20' } });
    fireEvent.change(reasonInput, { target: { value: 'Yarn spin mill delivery delayed' } });

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(auditApi.changeExFactoryDate).toHaveBeenCalledWith(101, '2026-05-20', 'Yarn spin mill delivery delayed');
      expect(onDateUpdated).toHaveBeenCalledWith('2026-05-20');
    });
  });
});

describe('BuyerWarningPanel Component', () => {
  const mockWarnings: BuyerDelayWarning[] = [
    {
      id: 301,
      orderId: 101,
      poNumber: 'PO-99120',
      riskStatus: 'HIGH',
      suggestedMessage: 'Production schedule is currently delayed by 5 days. Extra sewing lines added.',
      internalReasons: 'Machine breakdown on line 4, spare part pending from Coimbatore',
      status: 'PENDING_CONFIRMATION',
      confirmedMessage: null,
      confirmedByEmail: null,
      confirmedAt: null,
      createdAt: '2026-03-15T09:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders warning list with pending confirmation item and hides internal reasons from buyer', async () => {
    vi.mocked(warningsApi.getWarnings).mockResolvedValue(mockWarnings);

    render(<BuyerWarningPanel orderId={101} poNumber="PO-99120" />);

    expect(screen.getByTestId('buyer-warning-panel')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByTestId('merch-warning-card-301')).toBeDefined();
      expect(screen.getByText(/Machine breakdown on line 4/)).toBeDefined();
      expect(screen.getByText(/Production schedule is currently delayed by 5 days/)).toBeDefined();
    });
  });

  it('allows merchandiser to review, edit and confirm warning for buyer release', async () => {
    vi.mocked(warningsApi.getWarnings).mockResolvedValue(mockWarnings);
    vi.mocked(warningsApi.confirmWarning).mockResolvedValue({
      ...mockWarnings[0],
      status: 'CONFIRMED',
      confirmedMessage: 'Revised customer advisory message: factory adding double shifts to meet ex-factory.',
      confirmedByEmail: 'merchandiser@threadflow.com',
      confirmedAt: '2026-03-15T10:00:00Z',
    });

    render(<BuyerWarningPanel orderId={101} poNumber="PO-99120" />);

    await waitFor(() => {
      expect(screen.getByTestId('start-confirm-btn-301')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('start-confirm-btn-301'));

    expect(screen.getByTestId('confirm-editor')).toBeDefined();
    const textarea = screen.getByTestId('confirm-message-textarea');
    fireEvent.change(textarea, { target: { value: 'Revised customer advisory message: factory adding double shifts to meet ex-factory.' } });

    fireEvent.click(screen.getByTestId('confirm-release-btn'));

    await waitFor(() => {
      expect(warningsApi.confirmWarning).toHaveBeenCalledWith(
        101,
        301,
        'Revised customer advisory message: factory adding double shifts to meet ex-factory.'
      );
    });
  });

  it('allows merchandiser to dismiss a false alarm warning', async () => {
    vi.mocked(warningsApi.getWarnings).mockResolvedValue(mockWarnings);
    vi.mocked(warningsApi.dismissWarning).mockResolvedValue({
      ...mockWarnings[0],
      status: 'DISMISSED',
    });

    render(<BuyerWarningPanel orderId={101} poNumber="PO-99120" />);

    await waitFor(() => {
      expect(screen.getByTestId('dismiss-btn-301')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('dismiss-btn-301'));

    await waitFor(() => {
      expect(warningsApi.dismissWarning).toHaveBeenCalledWith(101, 301);
    });
  });
});
