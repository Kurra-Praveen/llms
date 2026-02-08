import { useState } from 'react';
import {
  DocumentTextIcon,
  ArrowDownTrayIcon
} from '@heroicons/react/24/outline';
import { Button } from '@/components/ui';
import { PortfolioReport } from './PortfolioReport';
import { CollectionReport } from './CollectionReport';

type ReportTab = 'portfolio' | 'collections' | 'aging';

export function ReportsPage() {
  const [activeTab, setActiveTab] = useState<ReportTab>('portfolio');

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Reports & Analytics</h1>
          <p className="text-gray-500 mt-1">
            Insights into your lending portfolio performance
          </p>
        </div>
        <Button variant="outline" leftIcon={<ArrowDownTrayIcon className="h-4 w-4" />}>
          Export PDF
        </Button>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('portfolio')}
            className={`
              whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm
              ${activeTab === 'portfolio'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }
            `}
          >
            Portfolio Summary
          </button>
          <button
             onClick={() => setActiveTab('collections')}
             className={`
              whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm
              ${activeTab === 'collections'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }
            `}
          >
            Collections
          </button>
          <button
             onClick={() => setActiveTab('aging')}
             className={`
              whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm
              ${activeTab === 'aging'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }
            `}
          >
            Aging Report
          </button>
        </nav>
      </div>

      {/* Content */}
      <div className="min-h-[400px]">
        {activeTab === 'portfolio' && <PortfolioReport />}
        {activeTab === 'collections' && <CollectionReport />}
        {activeTab === 'aging' && (
          <div className="text-center py-12 text-gray-500 bg-white rounded-lg border border-gray-200">
            <DocumentTextIcon className="h-12 w-12 mx-auto text-gray-300 mb-3" />
            <h3 className="text-lg font-medium text-gray-900">Aging Report</h3>
            <p className="mt-1">Detailed aging analysis coming soon.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default ReportsPage;
