import { render, screen } from '@testing-library/react';
import App from './App';

test('renders data request processing system header', () => {
    render(<App />);
    const headerElement = screen.getByRole('heading', { name: /Data Request Processing System/i });
    expect(headerElement).toBeInTheDocument();
});

test('renders create new request form', () => {
    render(<App />);
    const formHeader = screen.getByRole('heading', { name: /Create New Request/i });
    expect(formHeader).toBeInTheDocument();
});

test('renders request type dropdown with correct options', () => {
    render(<App />);
    expect(screen.getByRole('option', { name: /Access/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Delete/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Correct/i })).toBeInTheDocument();
});