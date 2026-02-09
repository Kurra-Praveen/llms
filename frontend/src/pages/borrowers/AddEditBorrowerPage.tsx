import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { BorrowerForm } from '@/components/forms/BorrowerForm';
import { borrowerService } from '@/services/borrowerService';
import { Spinner } from '@/components/ui';

export function AddBorrowerPage() {
  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Add New Borrower</h1>
        <p className="text-gray-500 mt-1">
          Enter borrower details to create a new record
        </p>
      </div>
      <BorrowerForm />
    </div>
  );
}

export function EditBorrowerPage() {
  const { id } = useParams<{ id: string }>();

  const { data: borrower, isLoading } = useQuery({
    queryKey: ['borrower', id],
    queryFn: () => borrowerService.getById(id!),
    enabled: !!id,
  });

  if (isLoading) {
    return <Spinner fullScreen text="Loading borrower details..." />;
  }

  if (!borrower) {
    return <div>Borrower not found</div>;
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Edit Borrower</h1>
        <p className="text-gray-500 mt-1">Update borrower information</p>
      </div>
      <BorrowerForm initialData={borrower} isEdit />
    </div>
  );
}
