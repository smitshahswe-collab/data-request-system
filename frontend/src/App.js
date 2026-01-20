import React, { useState, useEffect } from 'react';
import './App.css';
import axios from "axios";

const API_BASE_URL = 'http://localhost:8080/data-requests';

function App() {
    const [requests, setRequests] = useState([]);
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [formData, setFormData] = useState({
        requestType: 'ACCESS',
        requesterId: '',
        notes: ''
    });
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        fetchRequests();
    }, []);

    const fetchRequests = async () => {
        try {
            const response = await axios.get(API_BASE_URL);
            setRequests(response.data);
            setError('');
        } catch (err) {
            setError('Failed to fetch requests: ' + err.message);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.requesterId.trim()) {
            setError('Requester ID is required');
            return;
        }

        try {
            await axios.post(API_BASE_URL, formData);
            setSuccess('Request created successfully!');
            setError('');
            setFormData({ requestType: 'ACCESS', requesterId: '', notes: '' });
            fetchRequests();
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError('Failed to create request: ' + err.message);
            setSuccess('');
        }
    };

    const handleInputChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const viewRequestDetails = async (id) => {
        try {
            const response = await axios.get(`${API_BASE_URL}/${id}`);
            setSelectedRequest(response.data);
            setError('');
        } catch (err) {
            setError('Failed to fetch request details: ' + err.message);
        }
    };

    const updateStatus = async (id, newStatus) => {
        try {
            await axios.put(`${API_BASE_URL}/${id}/status`, { status: newStatus });
            setSuccess('Status updated successfully!');
            setError('');
            fetchRequests();
            if (selectedRequest && selectedRequest.id === id) {
                viewRequestDetails(id);
            }
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setError('Failed to update status: ' + err.response?.data?.message || err.message);
            setSuccess('');
        }
    };

    const getStatusBadgeClass = (status) => {
        const classes = {
            'RECEIVED': 'status-received',
            'IN_REVIEW': 'status-review',
            'COMPLETED': 'status-completed',
            'REJECTED': 'status-rejected'
        };
        return classes[status] || '';
    };

    const getNextStatuses = (currentStatus) => {
        const transitions = {
            'RECEIVED': ['IN_REVIEW', 'REJECTED'],
            'IN_REVIEW': ['COMPLETED', 'REJECTED'],
            'COMPLETED': [],
            'REJECTED': []
        };
        return transitions[currentStatus] || [];
    };

    return (
        <div className="App">
            <header className="App-header">
                <h1>📋 Data Request Processing System</h1>
                <p>Privacy Request Management</p>
            </header>

            <div className="container">
                {error && <div className="alert alert-error">{error}</div>}
                {success && <div className="alert alert-success">{success}</div>}

                <div className="grid">
                    {/* Create Request Form */}
                    <div className="card">
                        <h2>Create New Request</h2>
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label>Request Type:</label>
                                <select
                                    name="requestType"
                                    value={formData.requestType}
                                    onChange={handleInputChange}
                                    required
                                >
                                    <option value="ACCESS">Access</option>
                                    <option value="DELETE">Delete</option>
                                    <option value="CORRECT">Correct</option>
                                </select>
                            </div>

                            <div className="form-group">
                                <label>Requester ID:</label>
                                <input
                                    type="text"
                                    name="requesterId"
                                    value={formData.requesterId}
                                    onChange={handleInputChange}
                                    placeholder="e.g., user123"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Notes (Optional):</label>
                                <textarea
                                    name="notes"
                                    value={formData.notes}
                                    onChange={handleInputChange}
                                    placeholder="Additional information..."
                                    rows="3"
                                />
                            </div>

                            <button type="submit" className="btn btn-primary">
                                Create Request
                            </button>
                        </form>
                    </div>

                    {/* Request List */}
                    <div className="card">
                        <h2>All Requests ({requests.length})</h2>
                        <div className="request-list">
                            {requests.length === 0 ? (
                                <p className="empty-state">No requests yet. Create one to get started!</p>
                            ) : (
                                requests.map(request => (
                                    <div
                                        key={request.id}
                                        className="request-item"
                                        onClick={() => viewRequestDetails(request.id)}
                                    >
                                        <div className="request-header">
                                            <span className="request-id">#{request.id}</span>
                                            <span className={`status-badge ${getStatusBadgeClass(request.status)}`}>
                        {request.status}
                      </span>
                                        </div>
                                        <div className="request-body">
                                            <strong>{request.requestType}</strong>
                                            <span className="requester">User: {request.requesterId}</span>
                                        </div>
                                        <div className="request-footer">
                                            {new Date(request.createdAt).toLocaleString()}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>

                    {/* Request Details */}
                    {selectedRequest && (
                        <div className="card details-card">
                            <h2>Request Details</h2>
                            <div className="details">
                                <div className="detail-row">
                                    <span className="detail-label">ID:</span>
                                    <span className="detail-value">#{selectedRequest.id}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Type:</span>
                                    <span className="detail-value">{selectedRequest.requestType}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Requester:</span>
                                    <span className="detail-value">{selectedRequest.requesterId}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Status:</span>
                                    <span className={`status-badge ${getStatusBadgeClass(selectedRequest.status)}`}>
                    {selectedRequest.status}
                  </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Created:</span>
                                    <span className="detail-value">
                    {new Date(selectedRequest.createdAt).toLocaleString()}
                  </span>
                                </div>
                                <div className="detail-row">
                                    <span className="detail-label">Last Updated:</span>
                                    <span className="detail-value">
                    {new Date(selectedRequest.lastUpdatedAt).toLocaleString()}
                  </span>
                                </div>
                                {selectedRequest.notes && (
                                    <div className="detail-row">
                                        <span className="detail-label">Notes:</span>
                                        <span className="detail-value">{selectedRequest.notes}</span>
                                    </div>
                                )}
                                {selectedRequest.aiSummary && (
                                    <div className="ai-summary">
                                        <div className="detail-label">🤖 AI Summary:</div>
                                        <div className="summary-content">{selectedRequest.aiSummary}</div>
                                    </div>
                                )}

                                {/* Status Update Buttons */}
                                {getNextStatuses(selectedRequest.status).length > 0 && (
                                    <div className="status-actions">
                                        <p className="detail-label">Update Status:</p>
                                        <div className="action-buttons">
                                            {getNextStatuses(selectedRequest.status).map(status => (
                                                <button
                                                    key={status}
                                                    onClick={() => updateStatus(selectedRequest.id, status)}
                                                    className="btn btn-secondary"
                                                >
                                                    Move to {status}
                                                </button>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default App;