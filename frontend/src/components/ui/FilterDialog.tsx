import { useState, useEffect, type ReactNode } from 'react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import { Button } from './Button';

export interface FilterField {
  key: string;
  label: string;
  type: 'select' | 'text' | 'date' | 'dateRange' | 'numberRange';
  options?: { value: string; label: string }[];
  placeholder?: string;
}

export interface FilterDialogProps {
  title: string;
  fields: FilterField[];
  values: Record<string, any>;
  onApply: (values: Record<string, any>) => void;
  onClose: () => void;
  onClear: () => void;
}

export function FilterDialog({
  title,
  fields,
  values,
  onApply,
  onClose,
  onClear,
}: FilterDialogProps) {
  const [localValues, setLocalValues] = useState<Record<string, any>>(values);

  useEffect(() => {
    setLocalValues(values);
  }, [values]);

  const handleChange = (key: string, value: any) => {
    setLocalValues((prev) => ({ ...prev, [key]: value }));
  };

  const handleApply = () => {
    onApply(localValues);
    onClose();
  };

  const handleClear = () => {
    setLocalValues({});
    onClear();
    onClose();
  };

  const renderField = (field: FilterField): ReactNode => {
    switch (field.type) {
      case 'select':
        return (
          <select
            value={localValues[field.key] || ''}
            onChange={(e) => handleChange(field.key, e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">All</option>
            {field.options?.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        );

      case 'text':
        return (
          <input
            type="text"
            value={localValues[field.key] || ''}
            onChange={(e) => handleChange(field.key, e.target.value)}
            placeholder={field.placeholder}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        );

      case 'date':
        return (
          <input
            type="date"
            value={localValues[field.key] || ''}
            onChange={(e) => handleChange(field.key, e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        );

      case 'dateRange':
        return (
          <div className="flex gap-2">
            <div className="flex-1">
              <label className="block text-xs text-gray-500 mb-1">From</label>
              <input
                type="date"
                value={localValues[`${field.key}From`] || ''}
                onChange={(e) => handleChange(`${field.key}From`, e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
            <div className="flex-1">
              <label className="block text-xs text-gray-500 mb-1">To</label>
              <input
                type="date"
                value={localValues[`${field.key}To`] || ''}
                onChange={(e) => handleChange(`${field.key}To`, e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
          </div>
        );

      case 'numberRange':
        return (
          <div className="flex gap-2">
            <div className="flex-1">
              <label className="block text-xs text-gray-500 mb-1">Min</label>
              <input
                type="number"
                value={localValues[`${field.key}Min`] || ''}
                onChange={(e) => handleChange(`${field.key}Min`, e.target.value)}
                placeholder="0"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
            <div className="flex-1">
              <label className="block text-xs text-gray-500 mb-1">Max</label>
              <input
                type="number"
                value={localValues[`${field.key}Max`] || ''}
                onChange={(e) => handleChange(`${field.key}Max`, e.target.value)}
                placeholder="No limit"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-start justify-center z-50 pt-20">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 animate-slide-down">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <XMarkIcon className="h-5 w-5 text-gray-500" />
          </button>
        </div>

        {/* Fields */}
        <div className="p-4 space-y-4 max-h-[60vh] overflow-y-auto">
          {fields.map((field) => (
            <div key={field.key}>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {field.label}
              </label>
              {renderField(field)}
            </div>
          ))}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-4 py-3 border-t bg-gray-50">
          <Button variant="outline" onClick={handleClear}>
            Clear All
          </Button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button onClick={handleApply}>Apply Filters</Button>
          </div>
        </div>
      </div>
    </div>
  );
}

// Helper to count active filters
export function countActiveFilters(values: Record<string, any>): number {
  return Object.values(values).filter(
    (v) => v !== '' && v !== null && v !== undefined
  ).length;
}

export default FilterDialog;
