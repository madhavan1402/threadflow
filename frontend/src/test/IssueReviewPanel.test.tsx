import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react';
import { IssueReviewPanel } from '../components/IssueReviewPanel';
import { issuesApi, type ClassificationResponse } from '../api/issues';

vi.mock('../api/issues', async () => {
  const actual = await vi.importActual<typeof import('../api/issues')>('../api/issues');
  return {
    ...actual,
    issuesApi: {
      pendingReview: vi.fn(),
      classification: vi.fn(),
      review: vi.fn(),
      reclassify: vi.fn(),
    },
  };
});

describe('IssueReviewPanel', () => {
  const sampleIssue: ClassificationResponse = {
    issueId: 101,
    productionUpdateId: 201,
    orderId: 301,
    poNumber: 'PO-7890',
    styleName: 'Pique Polo',
    supervisorId: 5,
    originalNote: 'இயந்திரம் பழுதாகிவிட்டது உடனடியாக சரிசெய்யவும்',
    detectedLanguage: 'ta',
    englishTranslation: 'Knitting machine broke down, please fix immediately.',
    predictedCategory: 'MACHINE_BREAKDOWN',
    confidence: 0.92,
    status: 'PENDING_REVIEW',
    providerId: 'openai-compatible',
    modelId: 'gpt-4o',
    reviewedCategory: null,
    reviewedTranslation: null,
    reviewerId: null,
    reviewerEmail: null,
    reviewedAt: null,
    reviewHistory: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders loading state initially', () => {
    vi.mocked(issuesApi.pendingReview).mockReturnValue(new Promise(() => {}));
    render(<IssueReviewPanel />);
    expect(screen.getByRole('status')).toBeDefined();
    expect(screen.getByText('Loading pending issue reviews...')).toBeDefined();
  });

  it('renders empty state when there are no pending issues', async () => {
    vi.mocked(issuesApi.pendingReview).mockResolvedValue([]);
    render(<IssueReviewPanel />);

    await waitFor(() => {
      expect(screen.getByText('No issues pending review')).toBeDefined();
    });
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeDefined();
  });

  it('displays API error and supports retry', async () => {
    vi.mocked(issuesApi.pendingReview).mockRejectedValueOnce(new Error('Backend unreachable'));
    render(<IssueReviewPanel />);

    await waitFor(() => {
      expect(screen.getByText('Issue reviews unavailable')).toBeDefined();
      expect(screen.getByText('Backend unreachable')).toBeDefined();
    });

    vi.mocked(issuesApi.pendingReview).mockResolvedValueOnce([sampleIssue]);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(screen.getByText('PO-7890')).toBeDefined();
    });
  });

  it('loads pending issue list and displays original Tamil note, AI category, and translation', async () => {
    vi.mocked(issuesApi.pendingReview).mockResolvedValue([sampleIssue]);
    render(<IssueReviewPanel />);

    await waitFor(() => {
      expect(screen.getByText('PO-7890')).toBeDefined();
    });

    // Original note preserved exactly in quote
    expect(screen.getAllByText('இயந்திரம் பழுதாகிவிட்டது உடனடியாக சரிசெய்யவும்').length).toBeGreaterThanOrEqual(1);
    // Language tag
    expect(screen.getByText('Detected: ta')).toBeDefined();
    // Confidence score
    expect(screen.getByText('92% confidence')).toBeDefined();
    // Predicted category
    expect(screen.getAllByText('Machine Breakdown').length).toBeGreaterThanOrEqual(1);
    // English translation
    expect(screen.getAllByText('Knitting machine broke down, please fix immediately.').length).toBeGreaterThanOrEqual(1);
  });

  it('accepts AI classification and updates UI with success', async () => {
    vi.mocked(issuesApi.pendingReview).mockResolvedValue([sampleIssue]);
    const reviewedIssue: ClassificationResponse = {
      ...sampleIssue,
      status: 'REVIEWED',
      reviewedCategory: 'MACHINE_BREAKDOWN',
      reviewedTranslation: sampleIssue.englishTranslation,
      reviewerId: 7,
      reviewerEmail: 'merch@threadflow.test',
      reviewedAt: new Date().toISOString(),
      reviewHistory: [
        {
          id: 1,
          decision: 'ACCEPTED_AI',
          category: 'MACHINE_BREAKDOWN',
          englishTranslation: sampleIssue.englishTranslation,
          reviewerId: 7,
          reviewerEmail: 'merch@threadflow.test',
          reviewedAt: new Date().toISOString(),
        },
      ],
    };
    vi.mocked(issuesApi.review).mockResolvedValue(reviewedIssue);

    render(<IssueReviewPanel />);

    await waitFor(() => {
      expect(screen.getByText('Accept AI classification')).toBeDefined();
    });

    fireEvent.click(screen.getByText('Accept AI classification'));

    await waitFor(() => {
      expect(issuesApi.review).toHaveBeenCalledWith(101, {
        acceptAiClassification: true,
      });
      expect(screen.getByText('AI classification accepted.')).toBeDefined();
    });
  });

  it('allows merchandiser to correct category and translation and saves review', async () => {
    vi.mocked(issuesApi.pendingReview).mockResolvedValue([sampleIssue]);
    const correctedIssue: ClassificationResponse = {
      ...sampleIssue,
      status: 'REVIEWED',
      reviewedCategory: 'POWER_FAILURE',
      reviewedTranslation: 'Power grid cut interrupted machine operation.',
      reviewerId: 7,
      reviewerEmail: 'merch@threadflow.test',
      reviewedAt: new Date().toISOString(),
      reviewHistory: [
        {
          id: 2,
          decision: 'CORRECTED',
          category: 'POWER_FAILURE',
          englishTranslation: 'Power grid cut interrupted machine operation.',
          reviewerId: 7,
          reviewerEmail: 'merch@threadflow.test',
          reviewedAt: new Date().toISOString(),
        },
      ],
    };
    vi.mocked(issuesApi.review).mockResolvedValue(correctedIssue);

    render(<IssueReviewPanel />);

    await waitFor(() => {
      expect(screen.getByText('PO-7890')).toBeDefined();
    });

    // Select corrected category
    const select = screen.getByLabelText('Issue category');
    fireEvent.change(select, { target: { value: 'POWER_FAILURE' } });

    // Edit translation
    const textarea = screen.getByLabelText('English translation');
    fireEvent.change(textarea, { target: { value: 'Power grid cut interrupted machine operation.' } });

    // Submit review
    fireEvent.click(screen.getByRole('button', { name: 'Save review' }));

    await waitFor(() => {
      expect(issuesApi.review).toHaveBeenCalledWith(101, {
        acceptAiClassification: false,
        category: 'POWER_FAILURE',
        englishTranslation: 'Power grid cut interrupted machine operation.',
      });
      expect(screen.getByText('Manual review saved.')).toBeDefined();
    });
  });

  it('renders reclassify button and allows reclassifying unavailable or failed issue', async () => {
    const unavailableIssue: ClassificationResponse = {
      ...sampleIssue,
      status: 'AI_UNAVAILABLE',
      predictedCategory: null,
      englishTranslation: null,
      confidence: null,
    };
    vi.mocked(issuesApi.pendingReview).mockResolvedValue([unavailableIssue]);
    vi.mocked(issuesApi.reclassify).mockResolvedValue({
      ...unavailableIssue,
      status: 'PENDING',
    });

    render(<IssueReviewPanel />);

    await waitFor(() => {
      expect(screen.getByText('AI service is currently unavailable or unconfigured. Manual classification is required.')).toBeDefined();
    });

    const retryButtons = screen.getAllByRole('button', { name: 'Retry AI classification' });
    fireEvent.click(retryButtons[0]);

    await waitFor(() => {
      expect(issuesApi.reclassify).toHaveBeenCalledWith(101);
      expect(screen.getByText('Issue reclassification triggered.')).toBeDefined();
    });
  });

  it('displays review history list when entries exist', async () => {
    const issueWithHistory: ClassificationResponse = {
      ...sampleIssue,
      reviewHistory: [
        {
          id: 11,
          decision: 'CORRECTED',
          category: 'QUALITY_ISSUE',
          englishTranslation: 'Yarn defects noted.',
          reviewerId: 7,
          reviewerEmail: 'lead.merch@threadflow.test',
          reviewedAt: '2026-11-09T10:30:00Z',
        },
      ],
    };
    vi.mocked(issuesApi.pendingReview).mockResolvedValue([issueWithHistory]);

    render(<IssueReviewPanel />);

    await waitFor(() => {
      expect(screen.getByText('Review history')).toBeDefined();
      expect(screen.getByText('Reviewer: lead.merch@threadflow.test')).toBeDefined();
      expect(screen.getByText('Yarn defects noted.')).toBeDefined();
    });
  });
});
