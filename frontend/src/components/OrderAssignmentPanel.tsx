import { useEffect, useMemo, useState } from 'react';
import { assignmentsApi, type AssignmentHistory, type OrderAssignments, type ProductionUnit, type Supervisor } from '../api/assignments';

export function OrderAssignmentPanel({ orderId }: { orderId: number }) {
  const [units, setUnits] = useState<ProductionUnit[]>([]);
  const [supervisors, setSupervisors] = useState<Supervisor[]>([]);
  const [assignment, setAssignment] = useState<OrderAssignments | null>(null);
  const [unitSupervisors, setUnitSupervisors] = useState<AssignmentHistory[]>([]);
  const [loadingUnitSupervisors, setLoadingUnitSupervisors] = useState(false);
  const [unitSupervisorError, setUnitSupervisorError] = useState('');
  const [selectedUnitId, setSelectedUnitId] = useState('');
  const [selectedSupervisorIds, setSelectedSupervisorIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingUnit, setSavingUnit] = useState(false);
  const [savingSupervisors, setSavingSupervisors] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const activeDirectIds = useMemo(() => new Set(
    assignment?.directAssignments.filter(item => item.active).map(item => item.supervisorId) || [],
  ), [assignment]);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [unitList, supervisorList, orderAssignments] = await Promise.all([
        assignmentsApi.units(), assignmentsApi.supervisors(), assignmentsApi.order(orderId),
      ]);
      setUnits(unitList);
      setSupervisors(supervisorList);
      setAssignment(orderAssignments);
      setSelectedUnitId(orderAssignments.productionUnit ? String(orderAssignments.productionUnit.id) : '');
      setSelectedSupervisorIds(orderAssignments.directAssignments.filter(item => item.active).map(item => item.supervisorId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Assignments could not be loaded.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, [orderId]);

  useEffect(() => {
    let current = true;
    if (!selectedUnitId) {
      setUnitSupervisors([]);
      setUnitSupervisorError('');
      return () => { current = false; };
    }
    setUnitSupervisors([]);
    setLoadingUnitSupervisors(true);
    setUnitSupervisorError('');
    assignmentsApi.unitSupervisors(Number(selectedUnitId)).then(result => {
      if (current) setUnitSupervisors(result.assignments);
    }).catch(cause => {
      if (current) setUnitSupervisorError(cause instanceof Error ? cause.message : 'Unit supervisors could not be loaded.');
    }).finally(() => {
      if (current) setLoadingUnitSupervisors(false);
    });
    return () => { current = false; };
  }, [selectedUnitId]);

  const saveUnit = async () => {
    setSavingUnit(true);
    setError('');
    setSuccess('');
    try {
      const saved = await assignmentsApi.setOrderUnit(orderId, selectedUnitId ? Number(selectedUnitId) : null);
      setAssignment(saved);
      setSuccess('Production unit assignment saved.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Production unit could not be saved.');
    } finally {
      setSavingUnit(false);
    }
  };

  const saveSupervisors = async () => {
    setSavingSupervisors(true);
    setError('');
    setSuccess('');
    try {
      const saved = await assignmentsApi.setOrderSupervisors(orderId, selectedSupervisorIds);
      setAssignment(saved);
      setSuccess('Individual supervisor assignments saved.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Supervisor assignments could not be saved.');
    } finally {
      setSavingSupervisors(false);
    }
  };

  const toggleSupervisor = (supervisorId: number) => {
    setSelectedSupervisorIds(current => current.includes(supervisorId)
      ? current.filter(id => id !== supervisorId)
      : [...current, supervisorId]);
  };

  if (loading) return <section className="assignment-panel" role="status">Loading order assignments...</section>;
  if (!assignment) return <section className="assignment-panel" aria-live="polite">
    <div className="section-heading"><div><p>Order access</p><h2>Supervisor assignments</h2></div></div>
    {error && <div className="login-alert" role="alert">{error}</div>}
    <button className="state-action" type="button" onClick={() => void load()}>Retry</button>
  </section>;

  const currentUnit = units.find(unit => String(unit.id) === selectedUnitId);
  const activeUnitSupervisors = unitSupervisors.filter(item => item.active);
  const activeSupervisorIds = new Set(supervisors.map(supervisor => supervisor.id));
  const supervisorChoices: Supervisor[] = [
    ...supervisors,
    ...assignment.directAssignments.filter(item => item.active && !activeSupervisorIds.has(item.supervisorId))
      .map(item => ({ id: item.supervisorId, email: item.supervisorEmail, enabled: false })),
  ];
  const hasUnitChange = selectedUnitId !== String(assignment.productionUnit?.id || '');
  const hasSupervisorChange = selectedSupervisorIds.length !== activeDirectIds.size
    || selectedSupervisorIds.some(id => !activeDirectIds.has(id));

  return (
    <section className="assignment-panel" aria-labelledby="assignment-title">
      <div className="section-heading"><div><p>Order access</p><h2 id="assignment-title">Supervisor assignments</h2></div></div>
      {error && <div className="login-alert" role="alert">{error}</div>}
      {success && <div className="success-alert" role="status">{success}</div>}
      <div className="assignment-unit-row">
        <label>Production unit
          <select value={selectedUnitId} onChange={event => setSelectedUnitId(event.target.value)}>
            <option value="">No production unit</option>
            {units.filter(unit => unit.active || String(unit.id) === selectedUnitId).map(unit => (
              <option key={unit.id} value={unit.id}>{unit.name} · {unit.code}</option>
            ))}
          </select>
        </label>
        <button className="state-action" type="button" onClick={() => void saveUnit()}
          disabled={!hasUnitChange || savingUnit || savingSupervisors}>
          {savingUnit ? 'Saving...' : 'Save unit'}
        </button>
      </div>
      {currentUnit && <div className="unit-membership-list">
        <strong>Supervisors in {currentUnit.name}</strong>
        {loadingUnitSupervisors ? <p className="assignment-empty" role="status">Loading unit supervisors...</p>
          : unitSupervisorError ? <p className="field-error" role="alert">{unitSupervisorError}</p>
          : activeUnitSupervisors.length === 0
          ? <p className="assignment-empty">No supervisors are actively assigned to this unit.</p>
          : <ul>{activeUnitSupervisors.map(item => <li key={item.id}>{item.supervisorEmail}</li>)}</ul>}
      </div>}
      <div className="individual-assignment-list">
        <div className="assignment-subheading"><strong>Direct order assignments</strong><span>Changes keep their history</span></div>
        {supervisorChoices.length === 0
          ? <p className="assignment-empty">No active supervisor accounts are available.</p>
          : <div className="assignment-checks">{supervisorChoices.map(supervisor => (
            <label key={supervisor.id}>
              <input type="checkbox" checked={selectedSupervisorIds.includes(supervisor.id)}
                disabled={savingUnit || savingSupervisors || (!supervisor.enabled && !selectedSupervisorIds.includes(supervisor.id))}
                onChange={() => toggleSupervisor(supervisor.id)} />
              <span>{supervisor.email}{supervisor.enabled ? '' : ' · inactive account'}</span>
            </label>
          ))}</div>}
        <button className="state-action" type="button" onClick={() => void saveSupervisors()}
          disabled={!hasSupervisorChange || savingUnit || savingSupervisors}>
          {savingSupervisors ? 'Saving...' : 'Save individual assignments'}
        </button>
      </div>
      <div className="assignment-history">
        <strong>Individual assignment history</strong>
        {assignment.directAssignments.length === 0
          ? <p className="assignment-empty">No individual order assignments yet.</p>
          : <ul>{assignment.directAssignments.map(item => (
            <li key={item.id} className={item.active ? 'assignment-active' : 'assignment-revoked'}>
              <span>{item.supervisorEmail}</span>
              <span>{item.active ? 'Active' : `Revoked ${item.revokedAt ? new Date(item.revokedAt).toLocaleDateString() : ''}`}</span>
            </li>
          ))}</ul>}
      </div>
    </section>
  );
}