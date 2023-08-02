-- Add a new 'wallet_id' column with a default auto-generated value
--ALTER TABLE public.connection_records ADD wallet_id UUID DEFAULT gen_random_uuid();
ALTER TABLE public.connection_records ADD wallet_id VARCHAR(4) DEFAULT '5678';

-- Enable RLS on the table (will not be active for the table owner)
ALTER TABLE public.connection_records ENABLE ROW LEVEL SECURITY;

-- Create a RLS policy that will be active for every users except for the table owner (not subject to RLS by default)
CREATE POLICY wallet_isolation_policy
    ON public.connection_records
    --USING (wallet_id = current_setting('app.current_wallet_id')::UUID);
    USING (wallet_id = current_setting('app.current_wallet_id'));